package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findBySettingKey(String settingKey);
    boolean existsBySettingKey(String settingKey);
}
