package com.neuroid.tracker.events

import android.text.Editable
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.ToggleButton
import androidx.appcompat.widget.SwitchCompat
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class NIDIdentityAllViewsTest {
    @Test
    fun identifyAllViews_edit_text() {
        val view = mockk<ViewGroup>()
        val editText = mockk<EditText>()
        val editTextEditable = mockk<Editable>()
        every { view.childCount } returns 1
        every { view.getChildAt(0) } returns editText
        every { view.contentDescription } returns "view group"
        every { editText.contentDescription } returns "edit text 1"
        every { editText.text } returns editTextEditable
        every { editText.parent } returns null
        every { editTextEditable.length } returns "this is edit text1 text".length
        every { editText.addTextChangedListener(any()) } just runs
        every { editText.getCustomSelectionActionModeCallback() } returns null
        every { editText.setCustomSelectionActionModeCallback(any()) } just runs
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = mockNeuroID()

        val registrationIdentificationHelper = RegistrationIdentificationHelper(nidMock, logger)
        registrationIdentificationHelper.identifySingleView(view, "someguid")
        verify {
            logger.d(
                "NID test output",
                "etn: INPUT, et: EditText, eid: edit text 1, v:S~C~~23",
            )
        }
    }

    @Test
    fun identifyAllViews_ToggleButton() {
        val view = mockk<ViewGroup>()
        val toggleButton = mockk<ToggleButton>()
        every { view.childCount } returns 1
        every { view.getChildAt(0) } returns toggleButton
        every { view.contentDescription } returns "view group"
        every { toggleButton.contentDescription } returns "toggle button 1"
        every { toggleButton.parent } returns null
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = mockNeuroID()

        val registrationIdentificationHelper = RegistrationIdentificationHelper(nidMock, logger)
        registrationIdentificationHelper.identifySingleView(view, "someguid")
        verify {
            logger.d(
                "NID test output",
                "etn: INPUT, et: ToggleButton, eid: toggle button 1, v:S~C~~0",
            )
        }
    }

    @Test
    fun identifyAllViews_SwitchCompat() {
        val view = mockk<ViewGroup>()
        val switchCompat = mockk<SwitchCompat>()
        every { view.childCount } returns 1
        every { view.getChildAt(0) } returns switchCompat
        every { view.contentDescription } returns "view group"
        every { switchCompat.contentDescription } returns "switch compat"
        every { switchCompat.parent } returns null
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = mockNeuroID()

        val registrationIdentificationHelper = RegistrationIdentificationHelper(nidMock, logger)
        registrationIdentificationHelper.identifySingleView(view, "someguid")
        verify {
            logger.d(
                "NID test output",
                "etn: INPUT, et: SwitchCompat, eid: switch compat, v:S~C~~0",
            )
        }
    }

    @Test
    fun identifyAllViews_ImageButton() {
        val view = mockk<ViewGroup>()
        val imageButton = mockk<ImageButton>()
        every { view.childCount } returns 1
        every { view.getChildAt(0) } returns imageButton
        every { view.contentDescription } returns "view group"
        every { imageButton.contentDescription } returns "image button"
        every { imageButton.parent } returns null
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = mockNeuroID()

        val registrationIdentificationHelper = RegistrationIdentificationHelper(nidMock, logger)
        registrationIdentificationHelper.identifySingleView(view, "someguid")
        verify {
            logger.d(
                "NID test output",
                "etn: INPUT, et: ImageButton, eid: image button, v:S~C~~0",
            )
        }
    }

    @Test
    fun identifyAllViews_SeekBar() {
        val view = mockk<ViewGroup>()
        val seekBar = mockk<SeekBar>()
        every { view.childCount } returns 1
        every { view.getChildAt(0) } returns seekBar
        every { view.contentDescription } returns "view group"
        every { seekBar.contentDescription } returns "seek bar"
        every { seekBar.parent } returns null
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = mockNeuroID()

        val registrationIdentificationHelper = RegistrationIdentificationHelper(nidMock, logger)
        registrationIdentificationHelper.identifySingleView(view, "someguid")
        verify { logger.d("NID test output", "etn: INPUT, et: SeekBar, eid: seek bar, v:S~C~~0") }
    }

    @Test
    fun identifyAllViews_RatingBar() {
        val view = mockk<ViewGroup>()
        val ratingBar = mockk<RatingBar>()
        every { view.childCount } returns 1
        every { view.getChildAt(0) } returns ratingBar
        every { view.contentDescription } returns "view group"
        every { ratingBar.contentDescription } returns "rating bar"
        every { ratingBar.parent } returns null
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = mockNeuroID()

        val registrationIdentificationHelper = RegistrationIdentificationHelper(nidMock, logger)
        registrationIdentificationHelper.identifySingleView(view, "someguid")
        verify {
            logger.d(
                "NID test output",
                "etn: INPUT, et: RatingBar, eid: rating bar, v:S~C~~0",
            )
        }
    }

    @Test
    fun identifyAllViews_radio_button_group() {
        val radioButtonViewParent4 = mockk<RadioGroup>()
        val radioButtonViewParent3 = mockk<RadioGroup>()
        val radioButtonViewParent2 = mockk<RadioGroup>()
        val radioButtonViewParent = mockk<RadioGroup>()
        val radioButton = mockk<RadioButton>()
        val radioGroup = mockk<RadioGroup>()
        val view = mockk<ViewGroup>()
        every { radioButtonViewParent4.id } returns 13
        every { radioButtonViewParent3.parent } returns radioButtonViewParent4
        every { radioButtonViewParent3.id } returns 12
        every { radioButtonViewParent2.parent } returns radioButtonViewParent3
        every { radioButtonViewParent2.id } returns 11
        every { radioButtonViewParent.parent } returns null
        every { radioButtonViewParent.contentDescription } returns "RadioGroupParent"
        every { radioButtonViewParent.id } returns 10
        every { view.contentDescription } returns "viewgroup"
        every { view.childCount } returns 1
        every { view.getChildAt(0) } returns radioGroup
        every { radioGroup.parent } returns null
        every { radioGroup.setOnHierarchyChangeListener(any()) } just runs
        every { radioGroup.checkedRadioButtonId } returns 12
        every { radioGroup.contentDescription } returns "RadioGroup"
        every { radioGroup.childCount } returns 1
        every { radioGroup.getChildAt(0) } returns radioButton
        every { radioButton.contentDescription } returns "RadioButton"
        every { radioButton.isChecked } returns true
        every { radioButton.parent } returns radioButtonViewParent
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = mockNeuroID()

        val registrationIdentificationHelper = RegistrationIdentificationHelper(nidMock, logger)
        registrationIdentificationHelper.identifySingleView(view, "someguid")
        verify { logger.d("NID test output", "etn: INPUT, et: RadioGroup, eid: RadioGroup, v:12") }
        verify(exactly = 2) {
            logger.d(
                "NID test output",
                "etn: INPUT, et: RadioButton, eid: RadioButton, v:true",
            )
        }
    }

    private fun mockNeuroID(): NeuroID {
        val nidMock = mockk<NeuroID>()
        every {
            nidMock.captureEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just runs

        return nidMock
    }
}
