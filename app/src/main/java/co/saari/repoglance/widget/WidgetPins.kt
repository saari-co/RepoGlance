package co.saari.repoglance.widget

import co.saari.repoglance.model.RepoRef

object WidgetPins {
    fun configurationList(catalogNames: List<String>, pins: Set<String>): List<RepoRef> {
        val ordered = catalogNames.filter { it in pins } + catalogNames.filterNot { it in pins }
        val known = ordered.toMutableList()
        pins.filterNot { it in known }.sorted().forEach { known.add(0, it) }
        return known.distinct().mapNotNull(::repoRefOrNull)
    }

    fun releasedRepositories(removed: Collection<String>, stillUsed: Collection<String>): Set<String> =
        removed.filterNot { it in stillUsed }.toSet()

    private fun repoRefOrNull(full: String): RepoRef? {
        val parts = full.split('/', limit = 2)
        if (parts.size != 2) return null
        return runCatching { RepoRef(parts[0], parts[1]) }.getOrNull()
    }
}
