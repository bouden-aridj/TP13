package com.example.tp13;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing an important location at Sidi Amar Campus.
 */
public class CampusLocation {
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final double latitude;
    private final double longitude;
    private final int iconResId;

    public CampusLocation(String id, String name, String description, String category,
                          double latitude, double longitude, int iconResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.iconResId = iconResId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getIconResId() {
        return iconResId;
    }

    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    /**
     * Helper method providing the default list of Sidi Amar Campus locations.
     */
    public static List<CampusLocation> getSidiAmarLocations() {
        List<CampusLocation> locations = new ArrayList<>();

        locations.add(new CampusLocation(
                "loc_admin",
                "Rectorat & Administration",
                "Main campus administration building housing student affairs and university rectorate.",
                "Administration",
                36.8185, 7.7050,
                R.drawable.ic_business
        ));

        locations.add(new CampusLocation(
                "loc_library",
                "Bibliothèque Centrale",
                "Central university library offering quiet study halls, digital archives, and book lending.",
                "Library",
                36.8170, 7.7060,
                R.drawable.ic_library
        ));

        locations.add(new CampusLocation(
                "loc_cs_dept",
                "Département Informatique",
                "Faculty of Sciences - Computer Science department, AI labs, and computer rooms.",
                "Department",
                36.8160, 7.7040,
                R.drawable.ic_school
        ));

        locations.add(new CampusLocation(
                "loc_restaurant",
                "Restaurant Universitaire",
                "Main student cafeteria providing lunch and dinner services for campus residents.",
                "Restaurant",
                36.8190, 7.7065,
                R.drawable.ic_restaurant
        ));

        locations.add(new CampusLocation(
                "loc_amphi",
                "Bloc Pédagogique & Amphis",
                "Main lecture complex containing large auditoriums (Amphi A-F) for core courses.",
                "Lecture Halls",
                36.8178, 7.7035,
                R.drawable.ic_lecture
        ));

        return locations;
    }
}