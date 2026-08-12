package co.saari.repoglance.data

import co.saari.repoglance.auth.GitHubAuthException
import co.saari.repoglance.auth.GitHubSession
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

class GitHubApiClient(
    private val session: GitHubSession,
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val clock: Clock = Clock.systemUTC(),
) {
    private val rateLimitLock = Any()
    private var nextAllowedAt: Instant? = null

    fun loadCatalog(): GitHubApiResult<LiveRepositoryCatalog> = authenticated {
        val viewerResponse = get("/user")
        var latestRateLimit = viewerResponse.rateLimit()
        val viewerJson = viewerResponse.requireObject()
        val viewer = GitHubViewer(
            login = viewerJson.requiredString("login"),
            avatarUrl = viewerJson.optionalString("avatar_url"),
        )

        val installations = mutableListOf<GitHubInstallation>()
        paginateObjectArray(
            path = "/user/installations",
            arrayName = "installations",
            pageSize = CATALOG_PAGE_SIZE,
            onResponse = { latestRateLimit = it.rateLimit() },
        ) { json ->
            val account = json.optionalObject("account")
            val ownerLogin = account?.optionalString("login")
                ?: account?.optionalString("slug")
                ?: account?.optionalString("name")
            installations += GitHubInstallation(
                id = json.requiredPositiveLong("id"),
                ownerLogin = ownerLogin,
            )
        }

        val repositoriesById = linkedMapOf<Long, LiveRepository>()
        installations.forEach { installation ->
            paginateObjectArray(
                path = "/user/installations/${installation.id}/repositories",
                arrayName = "repositories",
                pageSize = CATALOG_PAGE_SIZE,
                onResponse = { latestRateLimit = it.rateLimit() },
            ) { json ->
                val fullName = json.requiredString("full_name")
                val slash = fullName.indexOf('/')
                require(slash in 1 until fullName.lastIndex) { "Invalid repository identity" }
                val repository = LiveRepository(
                    id = json.requiredPositiveLong("id"),
                    ref = RepoRef(fullName.substring(0, slash), fullName.substring(slash + 1)),
                    isPrivate = json.requiredBoolean("private"),
                    isArchived = json.requiredBoolean("archived"),
                    pushedAt = json.optionalInstant("pushed_at"),
                )
                repositoriesById[repository.id] = repository
            }
        }

        SuccessPayload(
            value = LiveRepositoryCatalog(
                viewer = viewer,
                installations = installations.distinctBy(GitHubInstallation::id),
                repositories = repositoriesById.values.sortedBy { it.ref.full.lowercase() },
            ),
            rateLimit = latestRateLimit,
        )
    }

    fun loadRepositoryContent(
        repository: LiveRepository,
        viewerLogin: String,
    ): LiveRepositoryContent {
        val owner = pathEncode(repository.ref.owner)
        val name = pathEncode(repository.ref.name)
        val issues: GitHubApiResult<LivePage<LiveIssue>> = authenticated {
            val response = get(
                withPageSize(
                    "/repos/$owner/$name/issues?state=open&sort=updated&direction=desc",
                    ISSUES_PAGE_SIZE,
                ),
            )
            val rows = response.requireArray().objects()
                .filterNot { it.has("pull_request") }
                .map(::parseIssue)
            SuccessPayload(
                value = LivePage(rows = rows, hasMorePages = response.hasNextPage()),
                rateLimit = response.rateLimit(),
            )
        }
        val pullRequests: GitHubApiResult<LivePage<LivePullRequest>> =
            if (issues is GitHubApiResult.Failure && issues.needsNewSignIn) {
                issues
            } else {
                authenticated {
                    val response = get(
                        withPageSize(
                            "/repos/$owner/$name/pulls?state=open&sort=updated&direction=desc",
                            ISSUES_PAGE_SIZE,
                        ),
                    )
                    val rows = response.requireArray().objects()
                        .map { parsePullRequest(it, viewerLogin) }
                    SuccessPayload(
                        value = LivePage(rows = rows, hasMorePages = response.hasNextPage()),
                        rateLimit = response.rateLimit(),
                    )
                }
            }
        return LiveRepositoryContent(repository, issues, pullRequests)
    }

    private fun parseIssue(json: JSONObject): LiveIssue = LiveIssue(
        number = json.requiredPositiveInt("number"),
        title = json.requiredString("title"),
        author = json.optionalObject("user")?.optionalString("login") ?: "unknown",
        assignee = json.optionalObject("assignee")?.optionalString("login"),
        labels = json.labelsList("labels"),
        commentCount = json.optionalNonNegativeInt("comments"),
        updatedAt = json.requiredInstant("updated_at"),
        htmlUrl = json.requiredGitHubUrl("html_url"),
    )

    private fun parsePullRequest(json: JSONObject, viewerLogin: String): LivePullRequest {
        val requested = json.requestedReviewersList()
            .any { it.equals(viewerLogin, ignoreCase = true) }
        return LivePullRequest(
            number = json.requiredPositiveInt("number"),
            title = json.requiredString("title"),
            author = json.optionalObject("user")?.optionalString("login") ?: "unknown",
            assignee = json.optionalObject("assignee")?.optionalString("login"),
            labels = json.labelsList("labels"),
            isDraft = json.optionalBoolean("draft") ?: false,
            reviewRequestedFromViewer = requested,
            updatedAt = json.requiredInstant("updated_at"),
            htmlUrl = json.requiredGitHubUrl("html_url"),
        )
    }

    private fun paginateObjectArray(
        path: String,
        arrayName: String,
        pageSize: Int,
        onResponse: (HttpResponse) -> Unit,
        consume: (JSONObject) -> Unit,
    ) {
        var currentPath = withPageSize(path, pageSize)
        var page = 1
        val visited = mutableSetOf<String>()

        while (page <= MAX_PAGES) {
            if (!visited.add(currentPath)) {
                throw IllegalStateException("GitHub pagination looped")
            }

            val response = get(currentPath)
            onResponse(response)
            val values = response.requireObject().getJSONArray(arrayName).objects()
            values.forEach(consume)
            page += 1
            val next = response.nextLinkPath() ?: return
            currentPath = withPageSize(next, pageSize)
        }
        throw IllegalStateException("GitHub pagination exceeded the safety limit")
    }

    private fun get(path: String): HttpResponse {
        require(path.startsWith('/')) { "GitHub API path must be absolute" }
        val now = clock.instant()
        val blockedUntil = synchronized(rateLimitLock) {
            nextAllowedAt?.takeIf(now::isBefore).also { active ->
                if (active == null) nextAllowedAt = null
            }
        }
        if (blockedUntil != null) {
            throw RateLimitGuardException(blockedUntil)
        }

        val response = transport.execute(
            HttpRequest(
                method = "GET",
                url = API_ROOT + path,
                headers = mapOf(
                    "Accept" to "application/vnd.github+json",
                    "Authorization" to "Bearer ${session.accessToken()}",
                    "X-GitHub-Api-Version" to API_VERSION,
                    "User-Agent" to "RepoGlance-Android",
                ),
            ),
        )

        val rateLimit = response.rateLimit(clock.instant())
        if (rateLimit.bucket == RateLimitBucket.EXHAUSTED) {
            val retryAt = rateLimit.resetsAt ?: clock.instant().plusSeconds(DEFAULT_SECONDARY_BACKOFF_SECONDS)
            synchronized(rateLimitLock) {
                nextAllowedAt = listOfNotNull(nextAllowedAt, retryAt).maxOrNull()
            }
        }
        if (response.statusCode !in 200..299) throw ApiFailureException(response)
        return response
    }

    private fun <T> authenticated(block: () -> SuccessPayload<T>): GitHubApiResult<T> {
        return try {
            val payload = block()
            GitHubApiResult.Success(
                value = payload.value,
                observedAt = clock.instant(),
                rateLimit = payload.rateLimit,
            )
        } catch (failure: GitHubAuthException) {
            GitHubApiResult.Failure(
                message = failure.message ?: "GitHub sign-in failed",
                statusCode = null,
                rateLimit = RateLimitSnapshot(RateLimitBucket.UNKNOWN, null, null, null),
                needsNewSignIn = failure.needsNewSignIn,
            )
        } catch (failure: RateLimitGuardException) {
            GitHubApiResult.Failure(
                message = "GitHub's rate limit is exhausted",
                statusCode = 429,
                rateLimit = RateLimitSnapshot(
                    bucket = RateLimitBucket.EXHAUSTED,
                    remaining = 0,
                    limit = null,
                    resetsAt = failure.retryAt,
                ),
            )
        } catch (failure: ApiFailureException) {
            val rateLimit = failure.response.rateLimit()
            GitHubApiResult.Failure(
                message = when (failure.response.statusCode) {
                    401 -> "Your GitHub session needs to be renewed"
                    403, 429 -> if (rateLimit.bucket == RateLimitBucket.EXHAUSTED) {
                        "GitHub's rate limit is exhausted"
                    } else {
                        "GitHub did not allow this read"
                    }
                    404 -> "This repository is not available to RepoGlance"
                    else -> "GitHub returned ${failure.response.statusCode}"
                },
                statusCode = failure.response.statusCode,
                rateLimit = rateLimit,
                needsNewSignIn = failure.response.statusCode == 401,
            )
        } catch (_: Exception) {
            GitHubApiResult.Failure(
                message = "Could not refresh GitHub right now",
                statusCode = null,
                rateLimit = RateLimitSnapshot(RateLimitBucket.UNKNOWN, null, null, null),
            )
        }
    }

    private data class SuccessPayload<T>(val value: T, val rateLimit: RateLimitSnapshot)

    private class ApiFailureException(val response: HttpResponse) : Exception()

    private class RateLimitGuardException(val retryAt: Instant) : Exception()

    private fun HttpResponse.requireObject(): JSONObject = JSONObject(body)
    private fun HttpResponse.requireArray(): JSONArray = JSONArray(body)

    private fun HttpResponse.rateLimit(observedAt: Instant = clock.instant()): RateLimitSnapshot {
        val remaining = header("X-RateLimit-Remaining")?.toIntOrNull()
        val limit = header("X-RateLimit-Limit")?.toIntOrNull()
        val rateLimitReset = header("X-RateLimit-Reset")?.toLongOrNull()?.let(Instant::ofEpochSecond)
        val retryAfterReset = header("Retry-After")?.let { header ->
            header.toLongOrNull()?.let { observedAt.plusSeconds(it) }
                ?: runCatching {
                    ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                }.getOrNull()
        }
        val signaledRateLimit = statusCode == 429 || statusCode == 403 && (
            remaining != null && remaining <= 0 ||
                retryAfterReset != null ||
                body.contains("secondary rate limit", ignoreCase = true) ||
                body.contains("rate limit exceeded", ignoreCase = true)
            )
        val reset = listOfNotNull(rateLimitReset, retryAfterReset).maxOrNull()
            ?: if (signaledRateLimit) observedAt.plusSeconds(DEFAULT_SECONDARY_BACKOFF_SECONDS) else null

        val bucket = when {
            signaledRateLimit -> RateLimitBucket.EXHAUSTED
            remaining == null || limit == null -> RateLimitBucket.UNKNOWN
            remaining <= 0 -> RateLimitBucket.EXHAUSTED
            remaining.toDouble() / limit.coerceAtLeast(1) <= 0.1 -> RateLimitBucket.LOW
            else -> RateLimitBucket.OK
        }
        return RateLimitSnapshot(bucket, remaining, limit, reset)
    }

    private fun HttpResponse.hasNextPage(): Boolean = nextLinkPath() != null

    private fun HttpResponse.nextLinkPath(): String? {
        val links = header("Link") ?: return null
        return links
            .split(',')
            .map(String::trim)
            .firstOrNull { it.contains("rel=\"next\"") }
            ?.let(LINK_NEXT_URI_PATTERN::find)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::toApiPath)
    }

    private fun toApiPath(linkHeaderUrl: String): String {
        val uri = URI(linkHeaderUrl)
        require(uri.scheme == "https" && uri.host == "api.github.com") { "Blocked pagination destination" }
        val path = uri.path
        val query = uri.rawQuery
        return if (query.isNullOrBlank()) path else "$path?$query"
    }

    private fun withPageSize(path: String, pageSize: Int): String {
        return if (path.contains("per_page=")) path else {
            val separator = if ('?' in path) '&' else '?'
            "$path${separator}per_page=$pageSize"
        }
    }

    private fun JSONArray.labelsList(): List<String> = (0 until length()).mapNotNull { index ->
        when (val item = get(index)) {
            is String -> item.takeIf(String::isNotBlank)
            is JSONObject -> item.optionalString("name")
            else -> null
        }
    }

    private fun JSONObject.labelsList(name: String): List<String> =
        if (has(name) && !isNull(name)) getJSONArray(name).labelsList() else emptyList()

    private fun JSONObject.requestedReviewersList(): List<String> =
        optJSONArray("requested_reviewers")?.loginList() ?: emptyList()

    private fun JSONArray.loginList(): List<String> = (0 until length()).mapNotNull { index ->
        when (val item = get(index)) {
            is String -> item.takeIf(String::isNotBlank)
            is JSONObject -> item.optionalString("login")
            else -> null
        }
    }

    private fun JSONObject.requiredString(name: String): String =
        optionalString(name) ?: throw IllegalArgumentException("Missing $name")

    private fun JSONObject.optionalString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name).takeIf(String::isNotBlank) else null

    private fun JSONObject.requiredBoolean(name: String): Boolean {
        require(has(name) && !isNull(name)) { "Missing $name" }
        return getBoolean(name)
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

    private fun JSONObject.optionalBoolean(name: String): Boolean? =
        if (has(name) && !isNull(name)) getBoolean(name) else null

    private fun JSONObject.requiredPositiveLong(name: String): Long = getLong(name).also {
        require(it > 0L) { "$name must be positive" }
    }

    private fun JSONObject.requiredPositiveInt(name: String): Int = getInt(name).also {
        require(it > 0) { "$name must be positive" }
    }

    private fun JSONObject.optionalNonNegativeInt(name: String): Int? =
        if (has(name) && !isNull(name)) getInt(name).also { require(it >= 0) } else null

    private fun JSONObject.requiredInstant(name: String): Instant =
        Instant.parse(requiredString(name))

    private fun JSONObject.optionalInstant(name: String): Instant? =
        optionalString(name)?.let(Instant::parse)

    private fun JSONObject.optionalObject(name: String): JSONObject? =
        if (has(name) && !isNull(name)) getJSONObject(name) else null

    private fun JSONObject.requiredGitHubUrl(name: String): String = requiredString(name).also { value ->
        require(value.startsWith("https://github.com/")) { "Blocked GitHub link" }
    }

    private fun pathEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val API_ROOT = "https://api.github.com"
        const val API_VERSION = "2022-11-28"
        const val CATALOG_PAGE_SIZE = 100
        const val ISSUES_PAGE_SIZE = 30
        const val MAX_PAGES = 100
        const val DEFAULT_SECONDARY_BACKOFF_SECONDS = 60L
        val LINK_NEXT_URI_PATTERN = Regex("<([^>]+)>\\s*;\\s*rel=\"next\"")
    }
}
