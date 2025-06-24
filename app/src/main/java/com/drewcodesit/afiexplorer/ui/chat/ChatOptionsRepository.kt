package com.drewcodesit.afiexplorer.ui.chat

import com.drewcodesit.afiexplorer.database.preloaded.AFITopicDAO
import com.drewcodesit.afiexplorer.database.preloaded.AFITopics
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatOptionsRepository @Inject constructor(private val dao: AFITopicDAO) {

    fun getAllTopics(): Flow<List<AFITopics>> = dao.getAllTopics()
}