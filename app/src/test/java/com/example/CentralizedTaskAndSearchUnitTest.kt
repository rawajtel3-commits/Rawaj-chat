package com.example

import com.example.data.Cabin
import com.example.data.CabinTask
import com.example.data.CabinType
import com.example.data.TaskPriority
import com.example.data.TaskStatus
import com.example.ui.components.CentralizedTaskSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CentralizedTaskAndSearchUnitTest {

    @Test
    fun testCabinSearchByName() {
        val cabin = Cabin(
            id = "c1",
            name = "كبينة مشروع الذكاء الاصطناعي",
            description = "تطوير المعمارية والتصميم الهندسي",
            type = CabinType.WORK,
            memberCount = 8,
            participantTags = "@أحمد, @سارة, @طارق, #هندسة, #ذكاء_اصطناعي"
        )

        assertTrue(cabin.matchesSearch("الذكاء الاصطناعي"))
        assertTrue(cabin.matchesSearch("مشروع"))
        assertTrue(cabin.matchesSearch("معمارية"))
        assertFalse(cabin.matchesSearch("غير موجود"))
    }

    @Test
    fun testCabinSearchByParticipantTags() {
        val cabin = Cabin(
            id = "c1",
            name = "كبينة مشروع الذكاء الاصطناعي",
            description = "تطوير المعمارية",
            type = CabinType.WORK,
            memberCount = 8,
            participantTags = "@أحمد, @سارة, @طارق, #هندسة, #ذكاء_اصطناعي"
        )

        val tags = cabin.getParticipantTagsList()
        assertEquals(5, tags.size)
        assertTrue(tags.contains("@أحمد"))
        assertTrue(tags.contains("@سارة"))
        assertTrue(tags.contains("#هندسة"))

        // Search with @ prefix
        assertTrue(cabin.matchesSearch("@سارة"))
        assertTrue(cabin.matchesSearch("@طارق"))
        assertTrue(cabin.matchesSearch("#هندسة"))

        // Search without @ or # prefix
        assertTrue(cabin.matchesSearch("سارة"))
        assertTrue(cabin.matchesSearch("هندسة"))
        assertTrue(cabin.matchesSearch("أحمد"))
    }

    @Test
    fun testCentralizedTaskSortingAndFiltering() {
        val tasks = listOf(
            CabinTask(
                id = "t1",
                cabinId = "c1",
                title = "مراجعة كود التشفير",
                description = "تدقيق المفاتيح",
                assignee = "سارة",
                dueDate = "خلال يومين",
                priority = TaskPriority.LOW,
                status = TaskStatus.DONE,
                createdAt = 1000L
            ),
            CabinTask(
                id = "t2",
                cabinId = "c2",
                title = "شراء المستلزمات العائلية",
                description = "قائمة الأغراض",
                assignee = "خالد",
                dueDate = "اليوم",
                priority = TaskPriority.HIGH,
                status = TaskStatus.TODO,
                createdAt = 2000L
            ),
            CabinTask(
                id = "t3",
                cabinId = "c1",
                title = "إعداد خادم WebRTC",
                description = "ربط الـ Audio Pod",
                assignee = "طارق",
                dueDate = "غداً",
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.IN_PROGRESS,
                createdAt = 3000L
            )
        )

        // 1. Sort by Priority (HIGH -> MEDIUM -> LOW)
        val sortedByPriority = tasks.sortedWith(
            compareBy<CabinTask> {
                when (it.priority) {
                    TaskPriority.HIGH -> 0
                    TaskPriority.MEDIUM -> 1
                    TaskPriority.LOW -> 2
                }
            }
        )
        assertEquals("t2", sortedByPriority[0].id) // HIGH
        assertEquals("t3", sortedByPriority[1].id) // MEDIUM
        assertEquals("t1", sortedByPriority[2].id) // LOW

        // 2. Sort by Due Date (اليوم -> غداً -> يومين)
        val sortedByDueDate = tasks.sortedWith(
            compareBy<CabinTask> {
                when {
                    it.dueDate.contains("اليوم") -> 0
                    it.dueDate.contains("غداً") -> 1
                    it.dueDate.contains("يومين") -> 2
                    else -> 3
                }
            }
        )
        assertEquals("t2", sortedByDueDate[0].id) // اليوم
        assertEquals("t3", sortedByDueDate[1].id) // غداً
        assertEquals("t1", sortedByDueDate[2].id) // خلال يومين

        // 3. Filter by Cabin ID
        val c1Tasks = tasks.filter { it.cabinId == "c1" }
        assertEquals(2, c1Tasks.size)

        // 4. Filter by Completion status
        val completed = tasks.filter { it.status == TaskStatus.DONE }
        assertEquals(1, completed.size)
        assertEquals("t1", completed.first().id)
    }

    @Test
    fun testCentralizedTaskCompletionToggle() {
        val task = CabinTask(
            id = "t_toggle",
            cabinId = "c1",
            title = "اختبار اكتمال المهمة",
            description = "تبديل الحالة",
            assignee = "أنت",
            dueDate = "اليوم",
            priority = TaskPriority.HIGH,
            status = TaskStatus.TODO
        )

        // Toggle from TODO to DONE
        val completed = task.copy(status = TaskStatus.DONE)
        assertEquals(TaskStatus.DONE, completed.status)

        // Toggle from DONE back to TODO
        val reverted = completed.copy(status = TaskStatus.TODO)
        assertEquals(TaskStatus.TODO, reverted.status)
    }
}
