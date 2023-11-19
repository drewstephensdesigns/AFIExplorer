package com.drewcodesit.afiexplorer.utils

import com.drewcodesit.afiexplorer.model.FeaturedPubs
import com.drewcodesit.afiexplorer.model.Pubs



// notify the parent class when a main item in the RecyclerView is clicked.
// This allows the parent class to respond to the click event and perform some action,
// such as opening a detail view for the selected item.
interface MainClickListener {
    fun onMainPubsClickListener(pubs: Pubs)
    fun onFeaturedPubsClickListener(featured: FeaturedPubs)

}