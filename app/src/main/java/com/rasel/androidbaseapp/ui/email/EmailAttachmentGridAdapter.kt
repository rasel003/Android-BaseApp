/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rasel.androidbaseapp.ui.email

import androidx.recyclerview.widget.GridLayoutManager
import com.rasel.androidbaseapp.ui.common.EmailAttachmentAdapter
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.ui.email.EmailFragment.Companion.MAX_GRID_SPANS
import kotlin.random.Random

class EmailAttachmentGridAdapter(
    private val spans: Int
) : EmailAttachmentAdapter() {

    /**
     * A [GridLayoutManager.SpanSizeLookup] which randomly assigns a span count to each item
     * in this adapter.
     */
    val variableSpanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

        private var indexSpanCounts: List<Int> = emptyList()

        override fun getSpanSize(position: Int): Int {
            return indexSpanCounts[position]
        }

        private fun generateSpanCountForItems(count: Int): List<Int> {
            val list = mutableListOf<Int>()

            var rowSpansOccupied = 1
            repeat(count) {
                val size =  when(it){
                    0 -> 3
                    1 -> 1
                    2 -> 1
                    3 -> 1
                    4 -> 2
                    else ->  Random.nextInt(1, spans + 1 - rowSpansOccupied)
                }
                rowSpansOccupied += size
                if (rowSpansOccupied >= MAX_GRID_SPANS) rowSpansOccupied = 0
                list.add(size)
            }

            return list
        }

        override fun invalidateSpanIndexCache() {
            super.invalidateSpanIndexCache()
            indexSpanCounts = generateSpanCountForItems(itemCount)
        }
    }

    override fun getLayoutIdForPosition(position: Int): Int =
        R.layout.email_attachment_grid_item_layout
}