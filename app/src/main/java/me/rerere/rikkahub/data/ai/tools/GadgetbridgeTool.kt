package me.rerere.rikkahub.data.ai.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.gadgetbridge.GadgetbridgeReader

fun createGadgetbridgeTool(customPath: String = ""): Tool = Tool(
    name = "get_gadgetbridge_data",
    description = "Get health and fitness data from Gadgetbridge (wearable device companion app). " +
        "Returns step count, heart rate, sleep data, blood oxygen, stress, and calories. " +
        "Reads from Gadgetbridge's auto-exported database. " +
        "Requires storage permission and Gadgetbridge auto-export to be enabled.",
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                putJsonObject("data_type") {
                    put("type", "string")
                    put(
                        "description",
                        "Type of health data to retrieve: 'all' (default), 'steps', 'heart_rate', 'sleep', 'daily_summary'"
                    )
                    put("enum", kotlinx.serialization.json.buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("all"))
                        add(kotlinx.serialization.json.JsonPrimitive("steps"))
                        add(kotlinx.serialization.json.JsonPrimitive("heart_rate"))
                        add(kotlinx.serialization.json.JsonPrimitive("sleep"))
                        add(kotlinx.serialization.json.JsonPrimitive("daily_summary"))
                    })
                }
            }
        )
    },
    execute = { args ->
        val params = args.jsonObject
        val dataType = params["data_type"]?.jsonPrimitive?.content ?: "all"

        try {
            if (!GadgetbridgeReader.dbFileExists(customPath)) {
                return@Tool listOf(UIMessagePart.Text(
                    buildJsonObject {
                        put("success", false)
                        put("error", "Gadgetbridge database not found. Please enable auto-export in Gadgetbridge settings. Expected path: /sdcard/Download/手环/Gadgetbridge.db")
                    }.toString()
                ))
            }

            val result = when (dataType) {
                "steps" -> {
                    val summaries = GadgetbridgeReader.readDailySummaries(7, customPath)
                    val today = summaries.lastOrNull()
                    buildJsonObject {
                        put("success", true)
                        put("data_type", "steps")
                        put("today_steps", today?.steps ?: 0)
                        put("weekly_summaries", kotlinx.serialization.json.buildJsonArray {
                            summaries.forEach { s ->
                                add(buildJsonObject {
                                    put("date", s.date.toString())
                                    put("steps", s.steps)
                                    put("calories", s.calories ?: 0)
                                })
                            }
                        })
                    }.toString()
                }
                "heart_rate" -> {
                    val latest = GadgetbridgeReader.readLatestActivitySample(customPath)
                    val summaries = GadgetbridgeReader.readDailySummaries(7, customPath)
                    buildJsonObject {
                        put("success", true)
                        put("data_type", "heart_rate")
                        put("current_heart_rate", latest?.heartRate ?: 0)
                        put("daily_summaries", kotlinx.serialization.json.buildJsonArray {
                            summaries.forEach { s ->
                                add(buildJsonObject {
                                    put("date", s.date.toString())
                                    put("hr_max", s.hrMax ?: 0)
                                    put("hr_min", s.hrMin ?: 0)
                                    put("hr_avg", s.hrAvg ?: 0)
                                    put("hr_resting", s.hrResting ?: 0)
                                })
                            }
                        })
                    }.toString()
                }
                "sleep" -> {
                    val stages = GadgetbridgeReader.readLastNightSleepStages(customPath)
                    buildJsonObject {
                        put("success", true)
                        put("data_type", "sleep")
                        put("total_minutes", stages.size)
                        put("light_sleep_minutes", stages.count { it.stage == 2 })
                        put("deep_sleep_minutes", stages.count { it.stage == 3 })
                        put("rem_sleep_minutes", stages.count { it.stage == 4 })
                        put("stages", kotlinx.serialization.json.buildJsonArray {
                            stages.forEach { stage ->
                                add(buildJsonObject {
                                    put("timestamp", stage.timestamp)
                                    put("stage", stage.stage)
                                    put("stage_name", stage.stageName)
                                })
                            }
                        })
                    }.toString()
                }
                "daily_summary" -> {
                    val summaries = GadgetbridgeReader.readDailySummaries(7, customPath)
                    buildJsonObject {
                        put("success", true)
                        put("data_type", "daily_summary")
                        put("summaries", kotlinx.serialization.json.buildJsonArray {
                            summaries.forEach { s ->
                                add(buildJsonObject {
                                    put("date", s.date.toString())
                                    put("steps", s.steps)
                                    put("calories", s.calories ?: 0)
                                    put("hr_max", s.hrMax ?: 0)
                                    put("hr_min", s.hrMin ?: 0)
                                    put("hr_avg", s.hrAvg ?: 0)
                                    put("hr_resting", s.hrResting ?: 0)
                                    put("stress_avg", s.stressAvg ?: 0)
                                    put("spo2_avg", s.spo2Avg ?: 0)
                                })
                            }
                        })
                    }.toString()
                }
                else -> {
                    // "all" - return combined data
                    val latest = GadgetbridgeReader.readLatestActivitySample(customPath)
                    val summaries = GadgetbridgeReader.readDailySummaries(7, customPath)
                    val sleepStages = GadgetbridgeReader.readLastNightSleepStages(customPath)
                    val (spo2, stress) = GadgetbridgeReader.readLatestSpo2AndStress(customPath)
                    val today = summaries.lastOrNull()
                    buildJsonObject {
                        put("success", true)
                        put("data_type", "all")
                        // Current status
                        put("current_heart_rate", latest?.heartRate ?: 0)
                        put("current_spo2", spo2 ?: 0)
                        put("current_stress", stress ?: 0)
                        // Today's summary
                        put("today_steps", today?.steps ?: 0)
                        put("today_calories", today?.calories ?: 0)
                        // Sleep
                        put("sleep_total_minutes", sleepStages.size)
                        put("sleep_light_minutes", sleepStages.count { it.stage == 2 })
                        put("sleep_deep_minutes", sleepStages.count { it.stage == 3 })
                        put("sleep_rem_minutes", sleepStages.count { it.stage == 4 })
                        // Weekly summaries
                        put("weekly_summaries", kotlinx.serialization.json.buildJsonArray {
                            summaries.forEach { s ->
                                add(buildJsonObject {
                                    put("date", s.date.toString())
                                    put("steps", s.steps)
                                    put("calories", s.calories ?: 0)
                                    put("hr_max", s.hrMax ?: 0)
                                    put("hr_min", s.hrMin ?: 0)
                                    put("hr_avg", s.hrAvg ?: 0)
                                    put("stress_avg", s.stressAvg ?: 0)
                                    put("spo2_avg", s.spo2Avg ?: 0)
                                })
                            }
                        })
                    }.toString()
                }
            }

            listOf(UIMessagePart.Text(result))
        } catch (e: Exception) {
            listOf(UIMessagePart.Text(
                buildJsonObject {
                    put("success", false)
                    put("error", e.message ?: "Unknown error reading Gadgetbridge data")
                }.toString()
            ))
        }
    }
)