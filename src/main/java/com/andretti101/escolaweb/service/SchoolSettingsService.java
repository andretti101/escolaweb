package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.SchoolSettings;

public interface SchoolSettingsService {
    SchoolSettings findSettings();
    SchoolSettings update(SchoolSettings settings);
}
