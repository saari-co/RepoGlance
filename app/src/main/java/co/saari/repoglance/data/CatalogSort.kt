package co.saari.repoglance.data

import java.time.Instant

enum class CatalogSort { RECENT, ALPHABETICAL }

fun orderRepositories(repositories: List<LiveRepository>, sort: CatalogSort): List<LiveRepository> = when (sort) {
    CatalogSort.ALPHABETICAL -> repositories.sortedBy { it.ref.full.lowercase() }
    CatalogSort.RECENT -> repositories.sortedWith(
        compareBy<LiveRepository> { it.pushedAt == null }
            .thenByDescending { it.pushedAt ?: Instant.EPOCH }
            .thenBy { it.ref.full.lowercase() },
    )
}

fun mostRecentlyPushed(repositories: List<LiveRepository>): LiveRepository? =
    orderRepositories(repositories, CatalogSort.RECENT).firstOrNull { it.pushedAt != null }
