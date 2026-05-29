package com.nicholasbergesen.gunsout.data.db

import androidx.room.TypeConverter
import com.nicholasbergesen.gunsout.data.entity.AlternateReason
import com.nicholasbergesen.gunsout.data.entity.DayHint
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.MealType
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.ProgramType
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.SessionStatus
import com.nicholasbergesen.gunsout.data.entity.SupplementUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {
    @TypeConverter fun localDateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter fun localDateTimeToString(value: LocalDateTime?): String? = value?.toString()
    @TypeConverter fun stringToLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter fun localTimeToString(value: LocalTime?): String? = value?.toString()
    @TypeConverter fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter fun equipmentToString(v: Equipment?): String? = v?.name
    @TypeConverter fun stringToEquipment(v: String?): Equipment? = v?.let(Equipment::valueOf)

    @TypeConverter fun muscleGroupToString(v: MuscleGroup?): String? = v?.name
    @TypeConverter fun stringToMuscleGroup(v: String?): MuscleGroup? = v?.let(MuscleGroup::valueOf)

    @TypeConverter fun alternateReasonToString(v: AlternateReason?): String? = v?.name
    @TypeConverter fun stringToAlternateReason(v: String?): AlternateReason? = v?.let(AlternateReason::valueOf)

    @TypeConverter fun protocolToString(v: Protocol?): String? = v?.name
    @TypeConverter fun stringToProtocol(v: String?): Protocol? = v?.let(Protocol::valueOf)

    @TypeConverter fun sessionStatusToString(v: SessionStatus?): String? = v?.name
    @TypeConverter fun stringToSessionStatus(v: String?): SessionStatus? = v?.let(SessionStatus::valueOf)

    @TypeConverter fun mealTypeToString(v: MealType?): String? = v?.name
    @TypeConverter fun stringToMealType(v: String?): MealType? = v?.let(MealType::valueOf)

    @TypeConverter fun supplementUnitToString(v: SupplementUnit?): String? = v?.name
    @TypeConverter fun stringToSupplementUnit(v: String?): SupplementUnit? = v?.let(SupplementUnit::valueOf)

    @TypeConverter fun programTypeToString(v: ProgramType?): String? = v?.name
    @TypeConverter fun stringToProgramType(v: String?): ProgramType? = v?.let(ProgramType::valueOf)

    @TypeConverter fun dayHintToString(v: DayHint?): String? = v?.name
    @TypeConverter fun stringToDayHint(v: String?): DayHint? = v?.let(DayHint::valueOf)
}
