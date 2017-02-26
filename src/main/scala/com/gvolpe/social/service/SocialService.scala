package com.gvolpe.social.service

import com.gvolpe.social.titan.{SocialNetworkTitanConfiguration, TitanConnection}

trait SocialService extends SocialNetworkTitanConfiguration {
  self: TitanConnection =>

}
