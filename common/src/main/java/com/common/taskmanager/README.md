# 统一任务管理系统

本文档介绍如何使用统一任务管理系统，该系统支持多种不同类型的任务表和对象。

## 设计目标

1. **支持多类型任务**：能够处理不同表和不同类型的任务对象
2. **统一状态管理**：维持一致的任务状态定义和生命周期
3. **易于扩展**：简单地添加新的任务类型和执行器
4. **解耦合**：任务管理器不直接依赖于具体的任务实现
5. **统一监听**：提供统一的事件通知机制
6. **懒加载机制**：按需创建适配器和执行器

## 系统架构

系统采用适配器模式，通过适配器将不同类型的任务转换为统一的操作接口。主要组件包括：

1. **TaskAdapter**：任务适配器接口，将不同类型的任务适配为统一接口
2. **TaskManager**：统一任务管理器，处理任务的添加、执行、取消和删除
3. **TaskExecutor**：任务执行器接口，负责执行特定类型的任务
4. **TaskEvent**：任务事件，封装任务和适配器信息
5. **TaskEventListener**：任务事件监听器，响应任务状态变化
6. **ListenerManager**：监听器管理类，管理和通知监听器
7. **TaskComponentFactory**：组件工厂，负责创建和管理适配器和执行器实例

## 使用方法

### 1. 初始化任务管理器

在应用启动时初始化统一任务管理器，并注册适配器和执行器：

```kotlin
// 获取任务管理器单例
val taskManager = TaskManager.getInstance()

// 注册适配器类
taskManager.registerAdapterClass(AITaskInfo::class.java, AITaskInfoAdapter::class.java)

// 注册其他类型的任务适配器
taskManager.registerAdapterClass(OtherTaskType::class.java, OtherTaskAdapter::class.java)

// 注册任务执行器类
taskManager.registerExecutorClass(TaskConstant.AI_TYPE_TEXT_TO_IMAGE, TextToImageExecutor::class.java)
taskManager.registerExecutorClass(TaskConstant.AI_TYPE_PICTURE_TO_VIDEO, PictureToVideoExecutor::class.java)

// 刷新任务列表（从数据库加载所有任务）
taskManager.refreshTasks()
```

### 2. 添加任务

添加任务非常简单，只需直接传入任务对象：

```kotlin
// 创建AITaskInfo类型的任务
val aiTask = AITaskInfo().apply {
    taskId = "task_${System.currentTimeMillis()}"
    type = TaskConstant.AI_TYPE_TEXT_TO_IMAGE
    status = TaskConstant.TASK_STATUS_CREATE
    // 设置其他必要属性...
}

// 添加到任务管理器
taskManager.addTask(aiTask)

// 添加其他类型的任务
val otherTask = OtherTaskType(
    id = "other_${System.currentTimeMillis()}",
    type = TaskConstant.AI_TYPE_PICTURE_TO_VIDEO
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
            TaskConstant.TASK_STATUS_SUCCESS -> {
                // 任务成功
            }
            TaskConstant.TASK_STATUS_FAILURE -> {
                // 任务失败
            }
            TaskConstant.TASK_STATUS_RUNNING -> {
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
taskManager.addTaskListener(listener, TaskConstant.AI_TYPE_TEXT_TO_IMAGE)

// 移除监听器
taskManager.removeTaskListener(listener)
```

### 4. 取消和删除任务

```kotlin
// 取消单个任务
taskManager.cancelTask(task)

// 取消所有任务
taskManager.cancelAllTask()

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
        TaskConstant.AI_TYPE_TEXT_TO_IMAGE
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
    
    override suspend fun updateTask(task: NewTask) {
        // 实现更新任务到数据库的逻辑
    }
    
    override suspend fun deleteTask(tasks: List<NewTask>) {
        // 实现从数据库删除任务的逻辑
    }
    
    override suspend fun loadAllTasks(): List<NewTask> {
        // 实现从数据库加载所有任务的逻辑
        return emptyList()
    }
    
    override suspend fun findTask(taskId: String): NewTask? {
        // 实现根据ID查找任务的逻辑
        return null
    }
    
    override fun isActive(task: NewTask): Boolean {
        // 判断任务是否处于活跃状态
        return task.status == TaskConstant.TASK_STATUS_CREATE || 
               task.status == TaskConstant.TASK_STATUS_RUNNING
    }
    
    override fun markStarted(task: NewTask) {
        // 标记任务为运行中状态
        task.status = TaskConstant.TASK_STATUS_RUNNING
    }
    
    override fun markSuccess(task: NewTask) {
        // 标记任务为成功状态
        task.status = TaskConstant.TASK_STATUS_SUCCESS
    }
    
    override fun markFailure(task: NewTask, message: String?) {
        // 标记任务为失败状态
        task.status = TaskConstant.TASK_STATUS_FAILURE
        task.errorMsg = message
    }
    
    override fun markDelete(task: NewTask) {
        // 标记任务为删除状态
        task.status = TaskConstant.TASK_STATUS_DELETE
    }
    
    override fun getTaskClass(): Class<NewTask> {
        return NewTask::class.java
    }
}
```

### 2. 创建执行器

为新的任务类型创建执行器，继承AbstractTaskExecutor：

```kotlin
class NewTaskExecutor : AbstractTaskExecutor<NewTask>() {
    override suspend fun doExecute(task: NewTask) {
        try {
            // 实现任务执行逻辑
            
            // 任务执行成功
            upDataSuccess(task)
        } catch (e: Exception) {
            // 任务执行失败
            upDataFailure(task)
        }
    }
    
    override fun getTaskClass(): Class<NewTask> {
        return NewTask::class.java
    }
}
```

### 3. 注册适配器和执行器

```kotlin
// 注册新的适配器类
taskManager.registerAdapterClass(NewTask::class.java, NewTaskAdapter::class.java)

// 注册新的执行器类
taskManager.registerExecutorClass(TaskConstant.NEW_TASK_TYPE, NewTaskExecutor::class.java)
```

## 任务状态定义

系统定义了以下任务状态：

```kotlin
// 任务状态定义
const val TASK_STATUS_DELETE = -1  // 已删除
const val TASK_STATUS_CREATE = 0   // 创建
const val TASK_STATUS_SUCCESS = 1  // 成功
const val TASK_STATUS_FAILURE = 2  // 失败
const val TASK_STATUS_RUNNING = 3  // 运行中
```

## 任务类型定义

系统支持以下任务类型：

```kotlin
// 任务类型定义
const val AI_TYPE_ORAL_BROADCASTING = 18  // AI口播
const val AI_TYPE_PICTURE_TO_VIDEO = 19   // 图生视频
const val AI_TYPE_TEXT_TO_IMAGE = 20      // 文生图
const val AI_TYPE_VIDEO_GENERATION = 21   // 视频生成
const val AI_TYPE_VIDEO_EDITING = 22      // 视频编辑
```

## 最佳实践

1. **适当分组**：将相关的任务类型分组到同一个执行器中
2. **适配器职责单一**：每个适配器只负责一种类型的任务
3. **执行器可共享**：一个执行器可以处理多种类型的任务
4. **错误处理**：在适配器和执行器中实现适当的错误处理逻辑
5. **避免阻塞**：任务执行应该是非阻塞的，使用协程处理耗时操作
6. **保持同步**：确保数据库和内存中的任务状态一致
7. **懒加载**：按需创建组件，避免不必要的资源消耗 