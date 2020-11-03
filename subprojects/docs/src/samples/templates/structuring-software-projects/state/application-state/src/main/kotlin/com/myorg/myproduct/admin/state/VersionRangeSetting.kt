package com.myorg.myproduct.admin.state

data class VersionRangeSetting(
        var minVersion: String = "",
        var maxVersion: String = ""
)