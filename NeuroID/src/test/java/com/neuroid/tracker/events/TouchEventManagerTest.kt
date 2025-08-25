package com.neuroid.tracker.events

import android.text.Editable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.ToggleButton
import androidx.appcompat.widget.SwitchCompat
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.models.NIDTouchModel
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTime
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class TouchEventManagerTest {
    @Test
    fun testShouldLogMoveEvent() {
        val mockNID = getMockedNeuroID()
        val mockedViewGroup = mockk<ViewGroup>()
        val mockedLogger = mockk<NIDLogWrapper>()
        val mockedTime = mockk<NIDTime>()
        every { mockedTime.getCurrentTimeMillis() } returns 50
        val tem = TouchEventManager(mockedViewGroup, mockNID, mockedLogger, mockedTime)
        assert(!tem.shouldRecordMoveEvent())
        assert(tem.hitCounter == 0)
        assert(tem.missCounter == 1)
        every {mockedTime.getCurrentTimeMillis() } returns 51
        assert(tem.shouldRecordMoveEvent())
        assert(tem.hitCounter == 1)
        assert(tem.missCounter == 1)
        every {mockedTime.getCurrentTimeMillis() } returns 100
        assert(!tem.shouldRecordMoveEvent())
        assert(tem.hitCounter == 1)
        assert(tem.missCounter == 2)
    }

    @Test
    fun testDetectCurrentView_EditText() {
        val mockChildView = mockk<EditText>()
        val mockEditable = mockk<Editable>()
        every {mockEditable.length} returns 10
        every {mockChildView.contentDescription} returns "content_description"
        every {mockChildView.text} returns mockEditable
        every {mockChildView.id} returns 12345
        testDetectView(
            mockChildView,
            type="TOUCH_START",
            tgs = "content_description",
            touches = listOf(NIDTouchModel(tid=0.0F, x=1.0F, y=1.0F)),
            m = "",
            v = "S~C~~10",
            actionType = MotionEvent.ACTION_DOWN)
    }

    @Test
    fun testDetectCurrentView_RadioButton_action_up() {
        val mockChildView = mockk<RadioButton>()
        val mockViewParent1 = mockk<RadioGroup>()
        val mockViewParent2 = mockk<RadioGroup>()
        every {mockChildView.parent} returns mockViewParent2
        every {mockViewParent2.parent} returns mockViewParent1
        every {mockViewParent1.contentDescription} returns "content_description"
        every {mockViewParent2.contentDescription} returns "content_description"
        every {mockChildView.contentDescription} returns "content_description"
        every {mockChildView.id} returns 12345
        testDetectView(
            mockChildView,
            type="TOUCH_END",
            tgs = "content_description",
            touches = listOf(NIDTouchModel(tid=0.0F, x=1.0F, y=1.0F)),
            m = "events_logged=0 events_not_logged=0",
            v = "",
            actionType = MotionEvent.ACTION_UP)
    }

    @Test
    fun testDetectCurrentView_RadioButton_action_move() {
        val mockChildView = mockk<RadioButton>()
        val mockViewParent1 = mockk<RadioGroup>()
        val mockViewParent2 = mockk<RadioGroup>()
        every {mockChildView.parent} returns mockViewParent2
        every {mockViewParent2.parent} returns mockViewParent1
        every {mockViewParent1.contentDescription} returns "content_description"
        every {mockViewParent2.contentDescription} returns "content_description"
        every {mockChildView.contentDescription} returns "content_description"
        every {mockChildView.id} returns 12345
        testDetectView(
            mockChildView,
            type="TOUCH_MOVE",
            tgs = "content_description",
            touches = listOf(NIDTouchModel(tid=0.0F, x=1.0F, y=1.0F)),
            m = "",
            v = "",
            actionType = MotionEvent.ACTION_MOVE)
    }

    private fun testDetectView(currentView: View, type: String,
                               tgs: String?, touches: List<NIDTouchModel>?,
                               m: String?, v: String?, actionType: Int) {
        val mockNID = getMockedNeuroID()
        val mockViewGroup = mockk<ViewGroup>()
        val mockChildView = currentView

        every {mockChildView.isLongClickable} returns true
        every {mockChildView.x } returns 10F
        every {mockChildView.y } returns 20F
        every { mockChildView.getLocationInWindow(any())} answers {
            val location = it.invocation.args[0] as IntArray
            location[0] = 1 // x coordinate
            location[1] = 1 // y coordinate
        }
        every {mockChildView.width} returns 100
        every {mockChildView.height} returns 100

        every { mockViewGroup.childCount } returns 1
        every { mockViewGroup.getChildAt(0) } returns mockChildView
        every { mockViewGroup.getLocationInWindow(any())} answers {
            val location = it.invocation.args[0] as IntArray
            location[0] = 1 // x coordinate
            location[1] = 1 // y coordinate
        }

        val mockedLogger = mockk<NIDLogWrapper>()
        val mockedTime = mockk<NIDTime>()
        every { mockedTime.getCurrentTimeMillis() } returns 50
        val motionEvent = mockk<MotionEvent>()
        every { motionEvent.pointerCount } returns 1
        every { motionEvent.getPointerId(any()) } returns 1
        every { motionEvent.getPointerProperties(any(), any()) } just runs
        every { motionEvent.historySize } returns 1
        every { motionEvent.getHistoricalY(any(), any()) } returns 1F
        every { motionEvent.getHistoricalX(any(), any()) } returns 1F
        every { motionEvent.x } returns 1F
        every { motionEvent.y } returns 1F
        every { motionEvent.rawX } returns 1F
        every { motionEvent.rawY } returns 1F
        every { motionEvent.xPrecision } returns 1F
        every { motionEvent.yPrecision } returns 1F
        every { motionEvent.size } returns 1F
        every { motionEvent.pressure } returns 1F
        every { motionEvent.action } returns actionType
        val tem = TouchEventManager(mockViewGroup, mockNID, mockedLogger, mockedTime)
        val result = tem.detectView(motionEvent, 1000L)
        assert(result != null)
        if (actionType == MotionEvent.ACTION_MOVE) {
            verify(exactly = 0) {
                mockNID.captureEvent(
                    any(), type, any(), any(), any(), tgs, touches, any(), any(), any(),
                    v, any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), m, any(), any(), any(), any(), any(), any(), any()
                )
            }
        } else {
            verify(exactly = 1) {
                mockNID.captureEvent(
                    any(), type, any(), any(), any(), tgs, touches, any(), any(), any(),
                    v, any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), m, any(), any(), any(), any(), any(), any(), any()
                )
            }
        }
    }

    @Test
    fun testDetectChangesOnView() {
        val mockNID = getMockedNeuroID()
        val mockedViewGroup = mockk<ViewGroup>()
        val mockedLogger = mockk<NIDLogWrapper>()
        val mockedTime = mockk<NIDTime>()
        every { mockedTime.getCurrentTimeMillis() } returns 50
        val tem = TouchEventManager(mockedViewGroup, mockNID, mockedLogger, mockedTime)
        val view = mockk<View>()
        every {view.contentDescription} returns "content_description"
        tem.detectChangesOnView(view, 100L, MotionEvent.ACTION_UP)
        assert(tem.lastView == null)
        tem.detectChangesOnView(view, 100L, MotionEvent.ACTION_DOWN)
        assert(tem.lastView == view)
    }

    @Test
    fun testGetView_ViewGroup_Spinner() {
        val mockNID = getMockedNeuroID()
        val mockViewGroup = mockk<ViewGroup>()
        val mockChildView = mockk<Spinner>()
        every { mockChildView.getLocationInWindow(any())} answers {
            val location = it.invocation.args[0] as IntArray
            location[0] = 1 // x coordinate
            location[1] = 1 // y coordinate
        }
        every {mockChildView.width} returns 100
        every {mockChildView.height} returns 100

        every { mockViewGroup.childCount } returns 1
        every { mockViewGroup.getChildAt(0) } returns mockChildView
        every { mockViewGroup.getLocationInWindow(any())} answers {
            val location = it.invocation.args[0] as IntArray
            location[0] = 1 // x coordinate
            location[1] = 1 // y coordinate
        }

        val mockedLogger = mockk<NIDLogWrapper>()
        val mockedTime = mockk<NIDTime>()
        every { mockedTime.getCurrentTimeMillis() } returns 50
        val tem = TouchEventManager(mockViewGroup, mockNID, mockedLogger, mockedTime)

        // Test with Spinner
        val spinner = mockk<Spinner>()
        every { spinner.adapter } returns null
        assert(tem.getView(mockViewGroup, 10F, 20F) is Spinner)
    }

    @Test
    fun testGetView_ViewGroup_NonSpinner() {
        val mockNID = getMockedNeuroID()
        val mockViewGroup = mockk<ViewGroup>()
        val mockChildView = mockk<View>()
        every { mockChildView.getLocationInWindow(any())} answers {
            val location = it.invocation.args[0] as IntArray
            location[0] = 1 // x coordinate
            location[1] = 1 // y coordinate
        }
        every {mockChildView.width} returns 100
        every {mockChildView.height} returns 100

        every { mockViewGroup.childCount } returns 1
        every { mockViewGroup.getChildAt(0) } returns mockChildView
        every { mockViewGroup.getLocationInWindow(any())} answers {
            val location = it.invocation.args[0] as IntArray
            location[0] = 1 // x coordinate
            location[1] = 1 // y coordinate
        }

        val mockedLogger = mockk<NIDLogWrapper>()
        val mockedTime = mockk<NIDTime>()
        every { mockedTime.getCurrentTimeMillis() } returns 50
        val tem = TouchEventManager(mockViewGroup, mockNID, mockedLogger, mockedTime)

        // Test with View
        val view = mockk<Spinner>()
        every { view.adapter } returns null
        assert(tem.getView(mockViewGroup, 10F, 20F) is View)
    }

    /**
     * exercises generateXYValues(), generatePointerValues(),
     */
    @Test
    fun testGenerateMotionEvent() {
        val mockNID = getMockedNeuroID()
        val mockViewGroup = mockk<ViewGroup>()
        val mockChildView = mockk<Spinner>()
        every { mockChildView.getLocationInWindow(any())} answers {
            val location = it.invocation.args[0] as IntArray
            location[0] = 1 // x coordinate
            location[1] = 1 // y coordinate
        }
        every {mockChildView.width} returns 100
        every {mockChildView.height} returns 100

        every { mockViewGroup.childCount } returns 1
        every { mockViewGroup.getChildAt(0) } returns mockChildView
        every { mockViewGroup.getLocationInWindow(any())} answers {
            val location = it.invocation.args[0] as IntArray
            location[0] = 10 // x coordinate
            location[1] = 20 // y coordinate
        }

        val mockedLogger = mockk<NIDLogWrapper>()
        val mockedTime = mockk<NIDTime>()
        every { mockedTime.getCurrentTimeMillis() } returns 50
        val tem = TouchEventManager(mockViewGroup, mockNID, mockedLogger, mockedTime)
        val motionEvent = mockk<MotionEvent>()
        every { motionEvent.pointerCount } returns 1
        every { motionEvent.getPointerId(any()) } returns 1
        every { motionEvent.getPointerProperties(any(), any()) } just runs
        every { motionEvent.historySize } returns 1
        every { motionEvent.getHistoricalY(any(), any()) } returns 1F
        every { motionEvent.getHistoricalX(any(), any()) } returns 1F
        every { motionEvent.x } returns 1F
        every { motionEvent.y } returns 1F
        every { motionEvent.rawX } returns 1F
        every { motionEvent.rawY } returns 1F
        every { motionEvent.xPrecision } returns 1F
        every { motionEvent.yPrecision } returns 1F
        every { motionEvent.size } returns 1F
        every { motionEvent.pressure } returns 1F
        val motionEventValues = tem.generateMotionEventValues(motionEvent)
        assert(motionEventValues.toString() == "{pointerCount=1, pointers={0={mPropId=0, " +
                "mPropToolType=0, historicalX=[1.0], historicalY=[1.0]}}, yValues={y=1.0, " +
                "yP=1.0, yR=1.0, yCalc=1.0}, xValues={x=1.0, xP=1.0, xR=1.0, xCalc=1.0}, " +
                "pressure=1.0, hSize=1, size=1.0}")
    }

    @Test
    fun testDetectBasicAndroidViewType() {
        assert(detectBasicAndroidViewType( mockk<EditText>()) == 1)
        assert(detectBasicAndroidViewType( mockk<CheckBox>()) == 1)
        assert(detectBasicAndroidViewType( mockk<RadioButton>()) == 1)
        assert(detectBasicAndroidViewType( mockk<ToggleButton>()) == 1)
        assert(detectBasicAndroidViewType( mockk<Switch>()) == 1)
        assert(detectBasicAndroidViewType( mockk<SwitchCompat>()) == 1)
        assert(detectBasicAndroidViewType( mockk<ImageButton>()) == 1)
        assert(detectBasicAndroidViewType( mockk<SeekBar>()) == 1)
        assert(detectBasicAndroidViewType( mockk<Spinner>()) == 1)
        assert(detectBasicAndroidViewType( mockk<RatingBar>()) == 1)
        assert(detectBasicAndroidViewType( mockk<RadioGroup>()) == 1)
        assert(detectBasicAndroidViewType( mockk<Button>()) == 2)
        assert(detectBasicAndroidViewType( mockk<View>()) == 0)
    }
}