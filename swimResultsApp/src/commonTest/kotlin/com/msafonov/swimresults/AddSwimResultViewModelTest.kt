package com.msafonov.swimresults

import com.msafonov.swimresults.data.model.Constants
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.repository.SwimHistoryRepository
import com.msafonov.swimresults.viewmodel.AddSwimResultUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddSwimResultViewModelTest {
    @Test
    fun availableDistanceChoicesFollowExistingDomainRules() {
        val state = AddSwimResultUiState()
        assertEquals(Constants.FREESTYLE_LIST, state.distances)
        assertEquals(Constants.MEDLEY_LIST_SHORT,
            state.copy(style = SwimStyle.MEDLEY).distances)
        assertEquals(Constants.MEDLEY_LIST_LONG,
            state.copy(style = SwimStyle.MEDLEY, pool = PoolType.Long).distances)
    }

    @Test
    fun newFormCannotSaveUntilRequiredFieldsAndTimeAreValid() {
        val valid = AddSwimResultUiState(
            newCompetitionName = "Городские соревнования",
            swimDate = "2026-06-13",
            swimmer = Swimmer.Women,
            time = "1:23.45"
        )
        assertTrue(valid.canSave)
        assertFalse(valid.copy(time = "1:60.00").canSave)
        assertFalse(valid.copy(swimDate = "12.06.2026").canSave)
    }

    @Test
    fun formattedTimerInputIsValidAndParsesToCentiseconds() {
        val state = AddSwimResultUiState(
            newCompetitionName = "Городские соревнования",
            swimDate = "2026-06-13",
            time = "1:23.45"
        )

        assertTrue(state.canSave)
        assertEquals(8_345L, SwimHistoryRepository.parseTimeCentiseconds(state.time))
    }
}
