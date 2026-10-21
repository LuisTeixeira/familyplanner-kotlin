package com.luisteixeira.familyplanner.calendar

import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.household.HouseholdAccess

class CalendarModule(
    householdAccess: HouseholdAccess
) : AppModule {

    companion object{
        const val NAME = "calendar"
    }

    override val name = NAME


}