# 统一任务管理系统

本文档介绍如何使用新的统一任务管理系统，该系统支持多种不同类型的任务表和对象。

## 设计目标

1. **支持多类型任务**：能够处理不同表和不同类型的任务对象
2. **统一状态管理**：维持一致的任务状态定义和生命周期
3. **易于扩展**：简单地添加新的任务类型和执行器
4. **解耦合**：任务管理器不直接依赖于具体的任务实现
5. **统一监听**：提供统一的事件通知机制

## 系统架构

系统采用适配器模式，通过适配器将不同类型的任务转换为统一的操作接口。主要组件包括：

1. **TaskAdapter**：任务适配器接口，将不同类型的任务适配为统一接口
2. **UnifiedTaskManager**：统一任务管理器，处理任务的添加、执行、取消和删除
3. **TaskExecutor**：任务执行器接口，负责执行特定类型的任务
4. **TaskEvent**：任务事件，封装任务和适配器信息
5. **TaskEventListener**：任务事件监听器，响应任务状态变化
6. **ListenerManager**：监听器管理类，管理和通知监听器

## 使用方法

### 1. 初始化任务管理器

在应用启动时初始化统一任务管理器，并注册适配器和执行器：

```kotlin
// 获取任务管理器单例
val taskManager = UnifiedTaskManager.getInstance()

// 注册AITaskInfo适配器
taskManager.registerAdapter(AITaskInfo::class.java, AITaskInfoAdapter())

// 注册其他类型的任务适配器
taskManager.registerAdapter(OtherTaskType::class.java, OtherTaskAdapter())

// 注册任务执行器
taskManager.registerExecutor(TextToImageExecutor())
taskManager.registerExecutor(PictureToVideoExecutor())
taskManager.registerExecutor(OtherTypeExecutor())

// 刷新任务列表（从数据库加载所有任务）
taskManager.refreshTasks()
```

### 2. 添加任务

添加任务非常简单，只需直接传入任务对象：

```kotlin
// 创建AITaskInfo类型的任务
val aiTask = AITaskInfo().apply {
    taskId = "task_${System.currentTimeMillis()}"
    type = TaskType.AI_TYPE_TEXT_TO_IMAGE
    status = TaskType.TASK_STATUS_CREATE
    // 设置其他必要属性...
}

// 添加到任务管理器
taskManager.addTask(aiTask)

// 添加其他类型的任务
val otherTask = OtherTaskType(
    id = "other_${System.currentTimeMillis()}",
    type = TaskType.AI_TYPE_PICTURE_TO_VIDEO
)
taskManager.addTask(otherTask)
```

### 3. 监听任务事件

注册任务监听器，接收任务状态变化通知：

```kotlin
// 创建任务监听器
val listener = object : TaskEventListener {
    override fun onTaskStatusChanged(event: TaskEvent<*>) {
        val taskId = event.getTaskId()
        val status = event.getStatus()
        
        // 处理任务状态变化
        when (status) {
            TaskType.TASK_STATUS_SUCCESS -> {
                // 任务成功
            }
            TaskType.TASK_STATUS_FAILURE -> {
                // 任务失败
            }
            TaskType.TASK_STATUS_RUNNING -> {
                // 任务执行中
            }
        }
    }
    
    override fun onTaskAdded(event: TaskEvent<*>) {
        // 处理任务添加事件
    }
    
    override fun onTaskRemoved(events: List<TaskEvent<*>>) {
        // 处理任务删除事件
    }
}

// 注册全局监听器（监听所有类型任务）
taskManager.addTaskListener(listener)

// 监听特定类型的任务
taskManager.addTaskListener(listener, TaskType.AI_TYPE_TEXT_TO_IMAGE)
```

### 4. 取消和删除任务

```kotlin
// 取消任务
taskManager.cancelTask(task)

// 删除单个任务
taskManager.deleteTasks(listOf(task))

// 删除多个任务
taskManager.deleteTasks(selectedTasks)
```

### 5. 查询任务

```kotlin
// 异步获取特定类型的任务
launch {
    val textToImageTasks = taskManager.getTasksByType(
        AITaskInfo::class.java, 
        TaskType.AI_TYPE_TEXT_TO_IMAGE
    )
    // 处理查询结果...
}

// 获取待处理的任务
launch {
    val pendingTasks = taskManager.getPendingTasks(AITaskInfo::class.java)
    // 处理查询结果...
}
```

### 6. 清理资源

在不再需要任务管理器时释放资源：

```kotlin
// 销毁任务管理器
taskManager.destroy()
```

## 扩展新的任务类型

### 1. 创建适配器

为新的任务类型创建适配器，实现TaskAdapter接口：

```kotlin
class NewTaskAdapter : TaskAdapter<NewTask> {
    override fun getTaskId(task: NewTask): String = task.id
    
    override fun getType(task: NewTask): Int = task.taskType
    
    override fun getStatus(task: NewTask): Int = task.status
    
    override suspend fun saveTask(task: NewTask) {
        // 实现保存任务到数据库的逻辑
    }
    
    // 实现其他适配方法...
    
    override fun getTaskClass(): Class<NewTask> {
        return NewTask::class.java
    }
}
```

### 2. 创建执行器

为新的任务类型创建执行器，继承AbstractTaskExecutor：

```kotlin
class NewTaskExecutor : AbstractTaskExecutor() {
    override suspend fun <T> doExecute(
        task: T,
        adapter: TaskAdapter<T>,
        callback: TaskCallback<T>
    ) {
        // 实现任务执行逻辑
    }
    
    override fun getSupportedTaskTypes(): List<Int> {
        return listOf(TaskType.NEW_TASK_TYPE)
    }
    
    override fun isSupportedTaskType(taskType: Int): Boolean {
        return taskType == TaskType.NEW_TASK_TYPE
    }
}
```

### 3. 注册适配器和执行器

```kotlin
// 注册新的适配器
taskManager.registerAdapter(NewTask::class.java, NewTaskAdapter())

// 注册新的执行器
taskManager.registerExecutor(NewTaskExecutor())
```

## 最佳实践

1. **适当分组**：将相关的任务类型分组到同一个执行器中
2. **适配器职责单一**：每个适配器只负责一种类型的任务
3. **执行器可共享**：一个执行器可以处理多种类型的任务
4. **错误处理**：在适配器和执行器中实现适当的错误处理逻辑
5. **避免阻塞**：任务执行应该是非阻塞的，使用协程处理耗时操作
6. **保持同步**：确保数据库和内存中的任务状态一致 