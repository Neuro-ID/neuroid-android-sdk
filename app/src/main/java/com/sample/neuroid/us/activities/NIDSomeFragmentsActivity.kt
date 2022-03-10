package com.sample.neuroid.us.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.sample.neuroid.us.R
import com.sample.neuroid.us.constants.NID_FINISH_ACTIVITY
import com.sample.neuroid.us.constants.NID_GO_TO_SECOND_FRAG
import com.sample.neuroid.us.constants.NID_GO_TO_THIRD_FRAG
import com.sample.neuroid.us.fragments.NIDFirstFragmentDirections
import com.sample.neuroid.us.fragments.NIDSecondFragmentDirections
import com.sample.neuroid.us.interfaces.NIDNavigateFragsListener

class NIDSomeFragmentsActivity:
    AppCompatActivity(),
    NIDNavigateFragsListener {
    private fun currentNavController(): NavController = findNavController(R.id.navHostFragmentTwo)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nid_activity_two)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.nid_fragment_one_title)
    }

    override fun onSupportNavigateUp(): Boolean {
        return when (currentNavController().currentDestination?.id) {
            R.id.fragment_one -> {
                finish()
                false
            }
            R.id.fragment_two -> {
                supportActionBar?.setTitle(R.string.nid_fragment_one_title)
                currentNavController().navigateUp()
            }
            R.id.fragment_three -> {
                supportActionBar?.setTitle(R.string.nid_fragment_two_title)
                currentNavController().navigateUp()
            }
            else -> currentNavController().navigateUp()
        }
    }

    override fun goToNextScreen(option: Int) {
        when(option) {
            NID_GO_TO_SECOND_FRAG -> {
                currentNavController().navigate(NIDFirstFragmentDirections.actionToFragmentTwo())
                supportActionBar?.setTitle(R.string.nid_fragment_two_title)
            }
            NID_GO_TO_THIRD_FRAG -> {
                currentNavController().navigate(NIDSecondFragmentDirections.actionToFragmentThree())
                supportActionBar?.setTitle(R.string.nid_fragment_three_title)
            }
            NID_FINISH_ACTIVITY -> finish()
        }
    }
}