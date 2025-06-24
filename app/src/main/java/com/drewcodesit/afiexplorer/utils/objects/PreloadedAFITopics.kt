package com.drewcodesit.afiexplorer.utils.objects

import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.preloaded.AFITopics

object PreloadedAFITopics {

    val preloadedAFITopics = listOf(
        AFITopics(
            90140,
            "Dress and Personnel Appearance",
            "Proper wear of approved uniform items, insignias, & decorations",
            "DAFI36-2903",
            "https://static.e-publishing.af.mil/production/1/af_a1/publication/dafi36-2903/dafi36-2903.pdf",
            R.drawable.ic_dress_appearance
        ),
        AFITopics(
            90410,
            "Military Leave Program",
            "The authority for chargeable and non-chargeable leave, as well as passes",
            "DAFI36-3003",
            "https://static.e-publishing.af.mil/production/1/af_a1/publication/dafi36-3003/dafi36-3003.pdf",
            R.drawable.ic_search
        ),
        AFITopics(
            2422,
            "The Tongue and Quill",
            "Guidelines for writing and speaking professionally",
            "AFH33-337",
            "https://static.e-publishing.af.mil/production/1/saf_ds/publication/afh33-337/afh33-337.pdf",
            R.drawable.ic_quill
        )
    )
}