package com.rasel.androidbaseapp.ui.plant_list

import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.HumanReadables
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.rasel.androidbaseapp.EspressoIdlingResourceRule
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.any
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.util.concurrent.TimeoutException


@MediumTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PlantListFragmentTest {

    private lateinit var navController: TestNavHostController
    val LIST_ITEM_IN_TEST = 0


    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get: Rule
    val espressoIdlingResourceRule = EspressoIdlingResourceRule()

//    private val navController: NavController = Mockito.mock(NavController::class.java)


    @Before
    fun setup() {
        hiltRule.inject()

        //Getting the NavController for test
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        launchFragmentInHiltContainer<PlantListFragment> {

            // Setting the navigation graph for the NavController
            navController.setGraph(R.navigation.nav_graph)

            //Sets the NavigationController for the specified View
            Navigation.setViewNavController(requireView(), navController)

            navController.navigate(R.id.nav_plantListFragment)

        }

        // Register the IdlingResource if you're using it
        IdlingRegistry.getInstance().register(RecyclerViewIdlingResource.getIdlingResource())

    }

    @Test
    fun a_test_isListFragmentVisible_onAppLaunch() {
        Espresso.onView(withId(R.id.plant_list))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun on_view_click_go_next_page() {

        Espresso.onView(withId(R.id.plant_list))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(withId(R.id.tvTest))
            .perform(ViewActions.click(), ViewActions.closeSoftKeyboard())

        // Verify that performing a click changes the NavController’s state
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.nav_plantDetailFragment)

    }

    @Test
    fun test_backNavigation_toPlantListFragment() {
        Espresso.onView(withId(R.id.plant_list))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(withId(R.id.tvTest)).perform(ViewActions.click())

        Espresso.onView(withId(R.id.plant_detail_name))
            .check(ViewAssertions.matches(withText("Bougainvillea")))
        /*  Mockito.verify(navController).navigate(
              PlantListFragmentDirections.actionPlantListFragmentToPlantDetailFragment(
                  "bougainvillea-glabra"
              )
          )*/
        /* Espresso.onView(withId(R.id.plant_detail_name)).perform(waitUntilVisible(5000L))
         // Confirm nav to DetailFragment and display title
        */

        /* assertEquals(
             navController.currentDestination?.id,
             R.id.nav_plantDetailFragment
         )*/

        navController.navigateUp()
//        Espresso.pressBack()
        // Confirm MovieListFragment in view
        Espresso.onView(withId(R.id.plant_list))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_backNavigation_toMovieListFragment() {

        // Click list item #LIST_ITEM_IN_TEST
        Espresso.onView(withId(R.id.plant_list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<PlantViewHolder>(
                    LIST_ITEM_IN_TEST,
                    ViewActions.click()
                )
            )

        // Confirm nav to DetailFragment and display title
        Espresso.onView(withId(R.id.plant_detail_name))
            .check(ViewAssertions.matches(withText("Apple")))

       /* Espresso.pressBack()

        // Confirm MovieListFragment in view
        Espresso.onView(withId(R.id.plant_list))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))*/
    }

    @Test
    fun clickAddShoppingItemButton_navigateToAddShoppingItemFragment() {
        // Increment IdlingResource before fetching data
        RecyclerViewIdlingResource.increment()

        // Perform actions to fetch data (e.g., perform a click or wait for the data to load)
        sleep(10000)

        // Decrement IdlingResource when data is loaded
        RecyclerViewIdlingResource.decrement()

//        Espresso.onView(withId(R.id.tvTest)).perform(ViewActions.click())
        /*Espresso.onView(withId(R.id.plant_list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<PlantAdapter.PlantViewHolder>(
                0,
                ViewActions.click()
            )
        )*/

        /* val position = 4
         Espresso.onView(withId(R.id.plant_list))
             .perform(
                 RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position),
                 RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                     position,
                     ViewActions.click()
                 )
             )*/

//        waitUntilLoaded { fragment.recycler }

        // Perform assertions on the RecyclerView
        /* onView(withId(R.id.plant_list))
             .check(matches(allOf(
                 withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                 isAssignableFrom(RecyclerView::class.java),
                 hasDescendant(withId(R.id.plant_item_image)) // Replace with the ID of a view within your RecyclerView item layout
             )))*/



        Espresso.onView(withId(R.id.plant_list)).perform(
            ViewActions.scrollTo(),
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                ViewActions.click()
            )
        );
        /*Mockito.verify(navController).navigate(
            PlantListFragmentDirections.actionPlantListFragmentToPlantDetailFragment(
                "bougainvillea-glabra"
            )
        )*/
    }

    /**
     * Stop the test until RecyclerView's data gets loaded.
     *
     * Passed [recyclerProvider] will be activated in UI thread, allowing you to retrieve the View.
     *
     * Workaround for https://issuetracker.google.com/issues/123653014
     */
    private inline fun waitUntilLoaded(crossinline recyclerProvider: () -> RecyclerView) {
        Espresso.onIdle()

        lateinit var recycler: RecyclerView

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recycler = recyclerProvider()
        }

        while (recycler.hasPendingAdapterUpdates()) {
            Thread.sleep(10)
        }
    }


    class WaitUntilVisibleAction(private val timeout: Long) : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return any(View::class.java)
        }

        override fun getDescription(): String {
            return "wait up to $timeout milliseconds for the view to become visible"
        }

        override fun perform(uiController: UiController, view: View) {

            val endTime = System.currentTimeMillis() + timeout

            do {
                if (view.visibility == View.VISIBLE) return
                uiController.loopMainThreadForAtLeast(50)
            } while (System.currentTimeMillis() < endTime)

            throw PerformException.Builder()
                .withActionDescription(description)
                .withCause(TimeoutException("Waited $timeout milliseconds"))
                .withViewDescription(HumanReadables.describe(view))
                .build()
        }
    }

    /**
     * @return a [WaitUntilVisibleAction] instance created with the given [timeout] parameter.
     */
    fun waitUntilVisible(timeout: Long): ViewAction {
        return WaitUntilVisibleAction(timeout)
    }

    @After
    fun tearDown() {
        // Unregister the IdlingResource if you're using it
        IdlingRegistry.getInstance().unregister(RecyclerViewIdlingResource.getIdlingResource())
    }
}

object RecyclerViewIdlingResource {

    private const val RESOURCE = "RECYCLER_VIEW_IDLING_RESOURCE"

    private val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        countingIdlingResource.decrement()
    }

    fun getIdlingResource(): IdlingResource {
        return countingIdlingResource
    }
}