package com.neuroid.tracker.events

import android.text.Editable
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.ToggleButton
import androidx.appcompat.widget.SwitchCompat
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.getMockedView
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
        val editText = mockk<EditText>()
        val editTextEditable = mockk<Editable>()
        val view = getMockedView(editText)

        every { editTextEditable.length } returns "this is edit text1 text".length

        every { editText.context } returns view.context
        every { editText.contentDescription } returns "edit text 1"
        every { editText.text } returns editTextEditable
        every { editText.parent } returns null
        every { editText.addTextChangedListener(any()) } just runs
        every { editText.getCustomSelectionActionModeCallback() } returns null
        every { editText.setCustomSelectionActionModeCallback(any()) } just runs
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = getMockedNeuroID()

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
        val toggleButton = mockk<ToggleButton>()
        val view = getMockedView(toggleButton)

        every { toggleButton.context } returns view.context
        every { toggleButton.contentDescription } returns "toggle button 1"
        every { toggleButton.parent } returns null

        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = getMockedNeuroID()

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
        val switchCompat = mockk<SwitchCompat>()
        val view = getMockedView(switchCompat)

        every { switchCompat.context } returns view.context
        every { switchCompat.contentDescription } returns "switch compat"
        every { switchCompat.parent } returns null

        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = getMockedNeuroID()

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
        val imageButton = mockk<ImageButton>()
        val view = getMockedView(imageButton)

        every { imageButton.context } returns view.context
        every { imageButton.contentDescription } returns "image button"
        every { imageButton.parent } returns null

        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = getMockedNeuroID()

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
        val seekBar = mockk<SeekBar>()
        val view = getMockedView(seekBar)

        every { seekBar.context } returns view.context
        every { seekBar.contentDescription } returns "seek bar"
        every { seekBar.parent } returns null

        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = getMockedNeuroID()

        val registrationIdentificationHelper = RegistrationIdentificationHelper(nidMock, logger)
        registrationIdentificationHelper.identifySingleView(view, "someguid")
        verify { logger.d("NID test output", "etn: INPUT, et: SeekBar, eid: seek bar, v:S~C~~0") }
    }

    @Test
    fun identifyAllViews_RatingBar() {
        val ratingBar = mockk<RatingBar>()
        val view = getMockedView(ratingBar)

        every { ratingBar.context } returns view.context
        every { ratingBar.contentDescription } returns "rating bar"
        every { ratingBar.parent } returns null

        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        val nidMock = getMockedNeuroID()

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
        val view = getMockedView(radioGroup)

        every { radioButton.context } returns view.context

        every { radioButtonViewParent4.id } returns 13
        every { radioButtonViewParent3.parent } returns radioButtonViewParent4
        every { radioButtonViewParent3.id } returns 12
        every { radioButtonViewParent2.parent } returns radioButtonViewParent3
        every { radioButtonViewParent2.id } returns 11
        every { radioButtonViewParent.parent } returns null
        every { radioButtonViewParent.contentDescription } returns "RadioGroupParent"
        every { radioButtonViewParent.id } returns 10

        every { radioGroup.context } returns view.context
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

        val nidMock = getMockedNeuroID()

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
}
