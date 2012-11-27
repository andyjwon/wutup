$(document).ready(function () {
	"use strict";
    var map, infowindow, baseUrl = window.location.protocol + "//" + window.location.hostname,
        calculateRadius = function (bounds) {
            if (Number.prototype.toRad === undefined) {
                Number.prototype.toRad = function () {
                    return this * Math.PI / 180;
                };
            }
            var R = 3959, // Statute Mile
                center = bounds.getCenter(),
                corner = bounds.getNorthEast(),
                lat1 = center.lat().toRad(),
                lat2 = corner.lat().toRad(),
                dLat = (lat2 - lat1).toRad(),
                dLon = (corner.lng() - center.lng()).toRad(),
                a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2),
                c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        },

        generateOccurrenceURL = function (center, radius, start, end) {
            return baseUrl + ':8080/wutup/occurrences?page=0&pageSize=20&center=' + center.lat() + ',' + center.lng() + '&radius=' + (radius <= 100 ? radius : 100) + '&start=' + start.getTime() +
                '&end=' + end.getTime();
        },

        displayInfoWindow = function (occurrence) {
            if (infowindow) {
                infowindow.close();
            }
            infowindow = new google.maps.InfoWindow({
                content: occurrence.event.name + '</br>',
                position: new google.maps.LatLng(occurrence.venue.latitude, occurrence.venue.longitude)
            });
            infowindow.open(map);
        },

        displayEventInfo = function (occurrence) {
            $("#result").html(function () {
                return "<h3>" + occurrence.event.name + "</h3>" + "<p>" + occurrence.event.description + "</p>" + "<h4>" + occurrence.venue.name + "</h4>" + "<p><b>Address:</b> " + occurrence.venue.address + "</p>" + "<p><b>Start:</b> " + occurrence.start + "</p>" + "<p><b>End:</b> " + occurrence.end + "</p>";
            });

        },

        createMarker = function (occurrence) {
            var marker = new google.maps.Marker({
                map: map,
                position: new google.maps.LatLng(occurrence.venue.latitude, occurrence.venue.longitude),
                title: occurrence.event.name,
                animation: google.maps.Animation.DROP
            });
            google.maps.event.addListener(marker, 'click', function () {
                displayInfoWindow(occurrence);
                displayEventInfo(occurrence);
            });
            return marker;
        },

        populateMap = function (gMap, events, mapMarkers) {
            var i,
                deleteOverlays = function () {
                    if (mapMarkers) {
                        for (i = 0; i < mapMarkers.length; i += 1) {
                            mapMarkers[i].setMap(null);
                        }
                    }
                    mapMarkers.length = 0;
                };
            map = gMap;
            deleteOverlays();
            for (i = 0; i < events.length; i += 1) {
                mapMarkers.push(createMarker(events[i]));
            }
        },

        parseOccurrencesForCalendar = function (occurrences) {
            var calendarEvents = [],
                i;
            for (i = 0; i < occurrences.length; i += 1) {
                calendarEvents.push({
                    title: occurrences[i].event.name,
                    start: new Date(occurrences[i].start),
                    end: new Date(occurrences[i].end),
                    event: occurrences[i].event,
                    venue: occurrences[i].venue,
                    allDay: false

                });
            }
            return calendarEvents;
        },

        populateCalendar = function (calendarEvents) {
            $('#calendar').fullCalendar('removeEventSource', calendarEvents);
            $('#calendar').fullCalendar('removeEvents');
            $('#calendar').fullCalendar('addEventSource', calendarEvents);
        },

        createCalendar = function (calendarEvents, map, mapMarkers) {
            var calendar = $('#calendar').fullCalendar({
                header: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'month,agendaWeek,agendaDay'
                },
                theme: true,
                editable: false,
                disableDragging: true,
                dayClick: function (date, allDay) {
                    if (allDay) {
                        calendar.fullCalendar('gotoDate', date.getFullYear(), date.getMonth(), date.getDate());
                        calendar.fullCalendar('changeView', 'agendaDay');
                    }
                },
                viewDisplay: function (view) { // This function controls behavior when the calendar view is changed
                    var bounds = map.getBounds(),
                        radius = calculateRadius(bounds);
                    $.getJSON(generateOccurrenceURL(bounds.getCenter(), radius, view.start, view.end), function (occurrences) {
                        calendarEvents = parseOccurrencesForCalendar(occurrences);
                        populateCalendar(calendarEvents);
                        populateMap(map, calendarEvents, mapMarkers);
                    });
                },
                eventClick: function (event) { //'event' is used here instead of 'occurrence' due to fullcalendar.js documentation
                    displayInfoWindow(event);
                    displayEventInfo(event);
                },
                events: calendarEvents
            });
        },

        getCalendarView = function () {
            return $('#calendar').fullCalendar('getView');
        },

        instantiateMapAndCalendar = function () {
            var initialLocation, siberia = new google.maps.LatLng(60, 105),
                newyork = new google.maps.LatLng(40.69847032728747, -73.9514422416687),
                browserSupportFlag = false,
                calendarEvents = null,
                mapOptions = {
                    zoom: 10,
                    mapTypeId: google.maps.MapTypeId.ROADMAP
                },
                mapMarkers = [],
                handleNoGeolocation = function (errorFlag) {
                    if (errorFlag === true) {
                        alert("Geolocation service failed.");
                        initialLocation = newyork;
                    } else {
                        alert("Your browser doesn't support geolocation. We've placed you in Siberia.");
                        initialLocation = siberia;
                        infowindow = new google.maps.InfoWindow({
                            map: map,
                            position: initialLocation,
                            content: 'Here\'s what\'s going on in Siberia...'
                        });
                    }
                    map.setCenter(initialLocation);
                };
            map = new google.maps.Map(document.getElementById("map_canvas"), mapOptions);

            // Trying W3C Geolocation
            if (navigator.geolocation) {
                browserSupportFlag = true;
                navigator.geolocation.getCurrentPosition(function (position) {
                    initialLocation = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
                    map.setCenter(initialLocation);
                    google.maps.event.addListenerOnce(map, 'idle', function () {
                        createCalendar(calendarEvents, map, mapMarkers);
                    });
                    var bounds, radius, center;
                    google.maps.event.addListener(map, 'idle', function () {
                        bounds = map.getBounds();
                        center = bounds.getCenter();
                        radius = calculateRadius(bounds);
                        $.getJSON(generateOccurrenceURL(center, radius, getCalendarView().start, getCalendarView().end), function (occurrences) {
                            calendarEvents = parseOccurrencesForCalendar(occurrences);
                            populateCalendar(calendarEvents);
                            populateMap(map, calendarEvents, mapMarkers);
                        });

                    });

                }, function () {
                    handleNoGeolocation(browserSupportFlag);
                });

                // Browser doesn't support Geolocation
            } else {
                browserSupportFlag = false;
                handleNoGeolocation(browserSupportFlag);
            }

        };


    instantiateMapAndCalendar();
    //Start your Engines!

});