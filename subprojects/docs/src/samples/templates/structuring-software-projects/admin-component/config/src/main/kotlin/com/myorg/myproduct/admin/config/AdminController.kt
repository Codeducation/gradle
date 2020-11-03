package com.myorg.myproduct.admin.config

import com.myorg.myproduct.admin.state.ConfigurationState

object AdminController {

    fun update(versionRange: VersionRange) {
        ConfigurationState.rangeSetting.minVersion = versionRange.fromVersion
        ConfigurationState.rangeSetting.maxVersion = versionRange.toVersion
    }
}