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

package com.rasel.androidbaseapp.ui.lessons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.materialstudies.owl.model.CourseRepo
import com.rasel.androidbaseapp.data.models.lessons
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.databinding.FragmentLessonBinding

/**
 * A [Fragment] displaying a lesson.
 */
class LessonFragment : Fragment() {

    private val args: LessonFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLessonBinding.inflate(inflater, container, false).apply {
            lesson = lessons.first()
            steps.apply {
                adapter = StepsAdapter(lessons, context)
                smoothScrollToPosition(args.stepNumber)
            }
            collapse.setOnClickListener {
                it.findNavController().popBackStack()
            }
        }
        (childFragmentManager.findFragmentById(R.id.lessons_sheet) as? LessonsSheetFragment)?.let {
            it.course = CourseRepo.getCourse(args.courseId)
        }
        return binding.root
    }
}
