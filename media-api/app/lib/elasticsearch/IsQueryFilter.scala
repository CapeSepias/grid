package lib.elasticsearch

import com.gu.mediaservice.lib.ImageFields
import com.gu.mediaservice.model._
import com.sksamuel.elastic4s.ElasticDsl.matchAllQuery
import com.sksamuel.elastic4s.requests.searches.queries.Query
import lib.MediaApiConfig
import org.joda.time.DateTime
import scalaz.syntax.std.list._

sealed trait IsQueryFilter extends Query with ImageFields {
  def query: Query

  override def toString: String = this match {
    case IsOwnedPhotograph(staffPhotographerOrg) => s"$staffPhotographerOrg-owned-photo"
    case IsOwnedIllustration(staffPhotographerOrg) => s"$staffPhotographerOrg-owned-illustration"
    case IsOwnedImage(staffPhotographerOrg) => s"$staffPhotographerOrg-owned"
    case _: IsDeleted => "deleted"
    case _: IsUnderQuota => "under-quota"
    case _: IsReapable => "reapable"
  }
}

object IsQueryFilter {
  // for readability, the client capitalises gnm, so `toLowerCase` it before matching
  def apply(value: String, overQuotaAgencies: () => List[Agency], config: MediaApiConfig): Option[IsQueryFilter] = {
    val organisation = config.staffPhotographerOrganisation.toLowerCase
    value.toLowerCase match {
      case s if s == s"$organisation-owned-photo" => Some(IsOwnedPhotograph(organisation))
      case s if s == s"$organisation-owned-illustration" => Some(IsOwnedIllustration(organisation))
      case s if s == s"$organisation-owned" => Some(IsOwnedImage(organisation))
      case "under-quota" => Some(IsUnderQuota(overQuotaAgencies()))
      case "deleted" => Some(IsDeleted(true))
      case "reapable" => Some(IsReapable(config.persistedRootCollections, config.persistenceIdentifier))
      case _ => None
    }
  }
}

case class IsOwnedPhotograph(staffPhotographerOrg: String) extends IsQueryFilter {
  override def query: Query = filters.or(
    filters.terms(usageRightsField("category"), UsageRights.photographer.toNel.get.map(_.category))
  )
}

case class IsOwnedIllustration(staffPhotographerOrg: String) extends IsQueryFilter {
  override def query: Query = filters.or(
    filters.terms(usageRightsField("category"), UsageRights.illustrator.toNel.get.map(_.category))
  )
}

case class IsOwnedImage(staffPhotographerOrg: String) extends IsQueryFilter {
  override def query: Query = filters.or(
    filters.terms(usageRightsField("category"), UsageRights.whollyOwned.toNel.get.map(_.category))
  )
}

case class IsUnderQuota(overQuotaAgencies: List[Agency]) extends IsQueryFilter {
  override def query: Query = overQuotaAgencies.toNel
    .map(agency => filters.mustNot(filters.terms(usageRightsField("supplier"), agency.map(_.supplier))))
    .getOrElse(matchAllQuery)
}

case class IsDeleted(isDeleted: Boolean) extends IsQueryFilter {
  override def query: Query = filters.or(
    (filters.existsOrMissing("softDeletedMetadata", _))(isDeleted)
  )
}

case class IsReapable(persistedRootCollections: List[String], persistenceIdentifier: String) extends IsQueryFilter {
  val moreThanTwentyDaysOld = filters.date("uploadTime", None, Some(DateTime.now().minusDays(20))).getOrElse(matchAllQuery())

  val persistedQueries = filters.or(
    PersistedQueries.hasCrops,
    PersistedQueries.usedInContent,
    PersistedQueries.addedToLibrary,
    PersistedQueries.hasUserEditsToImageMetadata,
    PersistedQueries.hasPhotographerUsageRights,
    PersistedQueries.hasIllustratorUsageRights,
    PersistedQueries.hasAgencyCommissionedUsageRights,
    PersistedQueries.addedToPhotoshoot,
    PersistedQueries.hasLabels,
    PersistedQueries.hasLeases,
    PersistedQueries.existedPreGrid(persistenceIdentifier),
    PersistedQueries.addedGNMArchiveOrPersistedCollections(persistedRootCollections)
  )
  override def query: Query = filters.and(
    moreThanTwentyDaysOld,
    filters.not(persistedQueries)
  )
}
