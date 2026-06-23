package cn.setbug.sub2apiusage

import android.content.Context
import java.io.File

const val SITES_FILE_NAME = "sub2api-sites.json"

fun siteStoreFile(context: Context): File {
    return File(context.applicationContext.filesDir, SITES_FILE_NAME)
}

fun siteRepository(context: Context): SiteRepository {
    return SiteRepository(siteStoreFile(context))
}
