package com.luisteixeira.familyplanner.calendar

import com.luisteixeira.familyplanner.calendar.api.CalendarApiHandler
import com.luisteixeira.familyplanner.calendar.web.CalendarWebHandler
import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.household.HouseholdAccess
import com.luisteixeira.familyplanner.household.householdScoped
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class CalendarModule(
    private val householdAccess: HouseholdAccess,
    private val webHandler: CalendarWebHandler,
    private val apiHandler: CalendarApiHandler,
) : AppModule {

    companion object{
        const val NAME = "calendar"
    }

    override val name = NAME

    override fun registerProtectedWebRoutes(r: Route) {
        r.route("/households/{householdID}/calendar") {
            householdScoped(householdAccess)
            get { webHandler.showWeek(call) }
            post("/events") {webHandler.createEvent(call)}
        }
    }

    override fun registerProtectedApiRoutes(r: Route) {
        r.route("/households/{householdID}/events") {
            householdScoped(householdAccess)
            get { apiHandler.listEvents(call) }
            post { apiHandler.createEvent(call) }
        }
    }
}