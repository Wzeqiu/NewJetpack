# 新版AI任务管理框架

## 架构设计

新版任务管理框架采用了更加模块化和可扩展的设计，主要包含以下核心组件：

1. **TaskManager**：单例核心管理器，负责任务的创建、执行、状态更新和删除等操作
2. **TaskExecutor**：任务执行器抽象类，定义了任务执行的通用接口
3. **TaskListener**：任务状态和进度监听接口
4. **TaskLifecycleListener**：任务生命周期监听接口

### 目录结构

根据功能类型进行了分包，使架构更加清晰：

```
com.mxm.douying.aigc.taskmanager/
├── core/                       # 核心类和接口
│   ├── TaskType.kt             # 任务类型和状态定义
│   ├── TaskExtensions.kt       # 任务扩展函数
│   ├── TaskManager.kt          # 任务管理器
│   └── TaskManagerExtensions.kt # 任务管理器扩展函数
├── listener/                   # 监听器相关
│   └── TaskListener.kt         # 任务监听器接口
├── helper/                     # 辅助工具类
│   ├── NetworkHelper.kt        # 网络操作辅助类
│   └── FileDownloadHelper.kt   # 文件下载辅助类
├── executor/                   # 执行器实现
│   ├── TaskExecutor.kt         # 任务执行器抽象类
│   ├── TextToImageExecutor.kt  # 文生图任务执行器
│   ├── VideoTaskExecutor.kt    # 视频类任务基础执行器
│   ├── PictureToVideoExecutor.kt # 图生视频任务执行器
│   └── OralBroadcastingExecutor.kt # AI口播任务执行器
└── README.md                   # 使用说明
```

## 特性

1. **统一的任务管理**：通过`TaskManager`统一管理所有类型的AI任务
2. **模块化设计**：每种任务类型有专门的执行器，便于扩展
3. **健壮的错误处理**：内置重试机制和异常捕获
4. **线程安全**：使用协程和互斥锁保证线程安全
5. **进度回调**：支持任务进度实时更新
6. **低内存占用**：优化内存使用，避免泄漏

## 使用方法

### 初始化

在应用启动时初始化任务管理器并加载任务：

```kotlin
// 在Application或合适的地方初始化
import com.common.taskmanager.core.TaskManager

val taskManager = TaskManager.getInstance()
taskManager.refreshTasks()
```

### 添加任务

```kotlin
// 创建一个新的AI任务
import com.common.taskmanager.core.TaskType

val task = AITaskInfo().apply {
    taskId = "task_${System.currentTimeMillis()}"
    type = TaskType.AI_TYPE_TEXT_TO_IMAGE
    status = TaskType.TASK_STATUS_CREATE
    createTime = System.currentTimeMillis()
    // 设置其他必要属性...
}

// 添加任务并自动执行
TaskManager.getInstance().addTask(task)
```

### 监听任务状态

```kotlin
// 监听任务状态变化
import com.common.taskmanager.listener.TaskListener

TaskManager.getInstance().addTaskStatusListener(object : TaskListener {
    override fun onTaskStatusChanged(task: AITaskInfo) {
        // 处理任务状态变化
        when (task.status) {
            TaskType.TASK_STATUS_SUCCESS -> {
                // 任务成功完成
            }
            TaskType.TASK_STATUS_FAILURE -> {
                // 任务失败
            }
        }
    }
    
    override fun onTaskProgressUpdated(task: AITaskInfo, progress: Int) {
        // 更新进度条等UI
        progressBar.progress = progress
    }
})

// 监听任务生命周期
import com.common.taskmanager.listener.TaskLifecycleListener

TaskManager.getInstance().addTaskLifecycleListener(object : TaskLifecycleListener {
    override fun onTaskAdded(task: AITaskInfo) {
        // 任务添加
    }
    
    override fun onTaskRemoved(tasks: List<AITaskInfo>) {
        // 任务删除
    }
})
```

### 获取任务列表

```kotlin
// 获取所有任务
val allTasks = TaskManager.getInstance().getTasks()

// 获取特定类型的任务
val textToImageTasks = TaskManager.getInstance().getTasksByType(TaskType.AI_TYPE_TEXT_TO_IMAGE)

// 获取待处理的任务
val pendingTasks = TaskManager.getInstance().getPendingTasks()

// 使用扩展函数检查是否有特定类型的任务正在运行
import com.common.taskmanager.core.hasRunningTextToImageTask

if (TaskManager.getInstance().hasRunningTextToImageTask()) {
    // 有文生图任务正在运行
}
```

### 删除任务

```kotlin
// 删除单个任务
TaskManager.getInstance().deleteTasks(listOf(task))

// 删除多个任务
TaskManager.getInstance().deleteTasks(selectedTasks)
```

### 清理资源

在不再需要时释放资源：

```kotlin
// 通常在应用退出时调用
TaskManager.getInstance().destroy()
```

## 迁移指南

从旧版任务管理系统迁移到新版系统时，需要注意以下几点：

1. 替换常量引用：将`AITask`中的常量替换为`TaskType`中的对应常量
2. 更新监听器：使用新的`TaskListener`和`TaskLifecycleListener`接口
3. 修改任务管理调用：将`TaskPlanInfoManager`的调用替换为`TaskManager`
4. 更新导入路径：注意更新为新的包结构中的正确路径

## 错误处理

框架内部已经处理了大部分错误情况，包括：

- 网络请求失败：自动重试，超过最大次数后回调失败
- 下载文件失败：自动重试，超过最大次数后回调失败
- 任务执行异常：捕获并处理异常，不会导致崩溃

## 性能优化

新框架在性能方面做了以下优化：

1. 使用协程替代传统线程，减少资源占用
2. 采用线程安全的集合类，避免并发问题
3. 使用ConcurrentHashMap和CopyOnWriteArrayList提高并发性能
4. 任务管理器单例设计，减少对象创建 