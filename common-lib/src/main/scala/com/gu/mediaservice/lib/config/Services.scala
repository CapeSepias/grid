package com.gu.mediaservice.lib.config

case class ServiceHosts(
  kahunaPrefix: String,
  apiPrefix: String,
  loaderPrefix: String,
  projectionPrefix: String,
  cropperPrefix: String,
  metadataPrefix: String,
  imgopsPrefix: String,
  usagePrefix: String,
  collectionsPrefix: String,
  leasesPrefix: String,
  authPrefix: String
)

object ServiceHosts {
  // this is tightly coupled to the Guardian's deployment.
  // TODO make more generic but w/out relying on Play config
  def guardianPrefixes: ServiceHosts = {
    val rootAppName: String = "media"

    ServiceHosts(
      kahunaPrefix = s"$rootAppName.",
      apiPrefix = s"api.$rootAppName.",
      loaderPrefix = s"loader.$rootAppName.",
      projectionPrefix = s"loader-projection.$rootAppName",
      cropperPrefix = s"cropper.$rootAppName.",
      metadataPrefix = s"$rootAppName-metadata.",
      imgopsPrefix = s"$rootAppName-imgops.",
      usagePrefix = s"$rootAppName-usage.",
      collectionsPrefix = s"$rootAppName-collections.",
      leasesPrefix = s"$rootAppName-leases.",
      authPrefix = s"$rootAppName-auth."
    )
  }
}

class Services(val domainRoot: String, hosts: ServiceHosts, corsAllowedOrigins: Set[String], domainRootOverride: Option[String] = None) {
  val kahunaHost: String      = s"${hosts.kahunaPrefix}$domainRoot"
  val apiHost: String         = s"${hosts.apiPrefix}$domainRoot"
  val loaderHost: String      = s"${hosts.loaderPrefix}${domainRootOverride.getOrElse(domainRoot)}"
  val cropperHost: String     = s"${hosts.cropperPrefix}${domainRootOverride.getOrElse(domainRoot)}"
  val metadataHost: String    = s"${hosts.metadataPrefix}${domainRootOverride.getOrElse(domainRoot)}"
  val imgopsHost: String      = s"${hosts.imgopsPrefix}${domainRootOverride.getOrElse(domainRoot)}"
  val usageHost: String       = s"${hosts.usagePrefix}${domainRootOverride.getOrElse(domainRoot)}"
  val collectionsHost: String = s"${hosts.collectionsPrefix}${domainRootOverride.getOrElse(domainRoot)}"
  val leasesHost: String      = s"${hosts.leasesPrefix}${domainRootOverride.getOrElse(domainRoot)}"
  val authHost: String        = s"${hosts.authPrefix}$domainRoot"
  val projectionHost: String  = s"${hosts.projectionPrefix}${domainRootOverride.getOrElse(domainRoot)}"

  val kahunaBaseUri      = baseUri(kahunaHost)
  val apiBaseUri         = baseUri(apiHost)
  val loaderBaseUri      = baseUri(loaderHost)
  val projectionBaseUri  = baseUri(projectionHost)
  val cropperBaseUri     = baseUri(cropperHost)
  val metadataBaseUri    = baseUri(metadataHost)
  val imgopsBaseUri      = baseUri(imgopsHost)
  val usageBaseUri       = baseUri(usageHost)
  val collectionsBaseUri = baseUri(collectionsHost)
  val leasesBaseUri      = baseUri(leasesHost)
  val authBaseUri        = baseUri(authHost)

  val allInternalUris = Seq(
    kahunaBaseUri,
    apiBaseUri,
    loaderBaseUri,
    cropperBaseUri,
    metadataBaseUri,
    usageBaseUri,
    collectionsBaseUri,
    leasesBaseUri,
    authBaseUri
  )

  val guardianWitnessBaseUri: String = "https://n0ticeapis.com"

  val corsAllowedDomains: Set[String] = corsAllowedOrigins.map(baseUri)

  val redirectUriParam = "redirectUri"
  val redirectUriPlaceholder = s"{?$redirectUriParam}"
  val loginUriTemplate = s"$authBaseUri/login$redirectUriPlaceholder"

  def baseUri(host: String) = s"https://$host"
}
