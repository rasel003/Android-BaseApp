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

package com.rasel.androidbaseapp.data.models

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import com.rasel.androidbaseapp.R

/**
 * An object which represents an account which can belong to a user. A single user can have
 * multiple accounts.
 */
data class Account(
    val id: Long,
    val uid: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val altEmail: String,
    val avatar: String,
    var isCurrentAccount: Boolean = false
) {
    val fullName: String = "$firstName $lastName"
    @DrawableRes val checkedIcon: Int = if (isCurrentAccount) R.drawable.ic_done else 0
}

object AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
    override fun areItemsTheSame(oldItem: Account, newItem: Account) =
        oldItem.email == newItem.email
    override fun areContentsTheSame(oldItem: Account, newItem: Account) =
        oldItem.firstName == newItem.firstName
            && oldItem.lastName == newItem.lastName
            && oldItem.avatar == newItem.avatar
            && oldItem.isCurrentAccount == newItem.isCurrentAccount
}
