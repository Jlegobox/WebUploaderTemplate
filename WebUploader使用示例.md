# Web Uploader入门看这一篇就可以了

> 本文用于记录在了解Web Uploader中总结出来的一些用法。主要参考了官方文档和社区中的一些回答并结合自己使用时的一些感受。在此提供一种我觉得更符合新手入门介绍文档。
>
> 官方文档：http://fex.baidu.com/webuploader/
>
> 完整大文件上传示例：https://github.com/Jlegobox/WebUploaderTemplate

### 简介

**引用官网的介绍**

WebUploader是由Baidu WebFE(FEX)团队开发的一个简单的以HTML5为主，FLASH为辅的现代文件上传组件。在现代的浏览器里面能充分发挥HTML5的优势，同时又不摒弃主流IE浏览器，沿用原来的FLASH运行时，兼容IE6+，iOS 6+, android 4+。两套运行时，同样的调用方式，可供用户任意选用。

采用大文件分片并发上传，极大的提高了文件上传效率。

### 引入Web Uploader

Web Uploader的引用只需要将两个文件`webuploader.js`和`webuploader.css`两个文件在html中引入。至于`Uploader.swf`需要在初始化中再引入。



### 初始化Web Uploader

Web Uploader的所有代码都在一个内部闭包中，对外暴露了唯一的一个变量`WebUploader`内部**所有**的类和功能都暴露在`WebUploader`名字空间下面。

有两种方法可以可以进行初始化，一种是使用`WebUploader.create`方法，另一种是直接访问`WebUploader.Uploader`。

```js
var uploader = WebUploader.create({
    swf: 'path_of_swf/Uploader.swf'

    // 其他配置项
});

var uploader = new WebUploader.Uploader({
    swf: 'path_of_swf/Uploader.swf'

    // 其他配置项
});


```

具体的可配置属性可以参考[API](http://fex.baidu.com/webuploader/doc/index.html#WebUploader_Uploader_options)。在程序中也可以调用`WebUploader.Uploader.optios`获取所有的参数配置。



### 配置事件

Web Uploader将上传中的步骤划分为多个事件，通过绑定事件的处理逻辑（`hander`）来在文件上传的过程中对文件进行处理。

`Uploader`实例具有Backbone同样的事件API：`on`，`off`，`once`，`trigger`。

```js
uploader.on( 'fileQueued', function( file ) {
    // do some things.
});
```

除了通过`on`绑定事件外，`Uploader`实例还有一个更便捷的添加事件方式。

```js
uploader.onFileQueued = function( file ) {
    // do some things.
};
```

如同`Document Element`中的`onEvent`一样，他的执行比`on`添加的`handler`的要晚。如果那些`handler`里面，有一个`return false`了，此`onEvent`里面是不会执行到的。

具体的事件可以参考[API](http://fex.baidu.com/webuploader/doc/index.html#WebUploader_Uploader_events)。要注意事件的发生节点和传入的参数。下面列举几个常用的关键事件：

- `beforeFileQueued` 当文件被加入队列之前触发，此事件的handler返回值为`false`，则此文件不会被添加进入队列。
- `fileQueued`当文件被加入队列以后触发。
- `startUpload` 当开始上传流程时触发。
- `uploadFinished` 当所有文件上传结束时触发。
- `uploadStart`某个文件开始上传前触发，一个文件只会触发一次。此时**文件还未分片**
- `uploadBeforeSend`当某个文件的**分块在发送前触发**，主要用来询问是否要添加附带参数，大文件在开起分片上传的前提下此事件可能会触发多次。
- `uploadComplete`不管成功或者失败，该文件上传完成时触发。



### 配置HOOK

除了配置事件之外，还可以配置钩子函数对文件进行处理。或者按照官方的说法：

>`Uploader`里面的功能被拆分成了好几个`widget`，由`command`机制来通信合作。
>
>`Uploader.regeister`方法用来说明，该`widget`要响应哪些命令，并指定由什么方法来响应。

可以手动的使用`request`进行命令的发送，从而调用钩子函数。也可以使用定义好的命令，这些命令在特定的步骤节点会被触发，与事件节点相同但是早于事件节点。

官方示例：

> filepicker在用户选择文件后，直接把结果`request`出去，然后负责队列的`queue` widget，监听命令，根据配置项中的`accept`来决定是否加入队列。

```js
// in file picker
picker.on( 'select', function( files ) {
    me.owner.request( 'add-file', [ files ]);
});

// in queue picker
Uploader.register({
    'add-file': 'addFiles'

    // xxxx
}, {

    addFiles: function( files ) {

        // 遍历files中的文件, 过滤掉不满足规则的。
    }
});
```

钩子函数是通过`WebUploader.Uploader.register()`进行注册的。同样有两种方式：

方式一：使用API名称与函数实现的映射map

```js
WebUploader.Uploader.register({
    'make-thumb': 'makeThumb'
}, {
    init: function( options ) {},
    makeThumb: function() {}
});
```

方式二：直接使用函数实现，但是函数名要与API名称相同

```js
WebUploader.Uploader.register({
    'make-thumb': function() {

    }
});
```

**注意：HOOK的注册要在WebUploader初始化之前，也就是逻辑要放在WebUploader之前才能生效。**

删除组件可以使用`WebUploader.Uploader.unRegister(name)`。并且只有注册时指定了名字的才能被删除

```js
Uploader.register({
    name: 'custom',

    'make-thumb': function() {

    }
});

Uploader.unRegister('custom');
```

为什么有了事件之后，我们还需要配置HOOK来进行处理呢？

因为设计的理念是，事件发生时进行命令的发布，而命令的处理是在HOOK中。

一个命令可以对应多个handler，各个handler会按照添加顺序一次执行，且后续的handler不能被前面的handler截断。

上述也可以通过事件中写逻辑达成。但是虽然事件的触发顺序可以保证，但是前面事件的异步处理结果在后面事件发生时不一定能获得。而HOOK提供了基于Promise对象的同步过程。保证handler在Promise处理结束后才结束，以此保证异步的操作完成后才进行下一步操作。

官方示例：

```js
// uploader在初始化的时候
me.request( 'init', opts, function() {
    me.state = 'ready';
    me.trigger('ready');
});

// filepicker `widget`中的初始化过程。
Uploader.register({
    'init': 'init'
}, {
    init: function( opts ) {

        var deferred = Base.Deferred();

        // 加载flash
        // 当flash ready执行deferred.resolve方法。

        return deferred.promise();
    }
});
```

目前Web Uploader内部有很多种command，在此列出比较重要的几个。

- `add-file`:用来向队列中添加文件。
- `before-send-file`:在文件发送之前request，此时还没有分片（如果配置了分片的话），可以用来做文件整体md5验证。
- `before-send`:在分片发送之前request，可以用来做分片验证，如果此分片已经上传成功了，可返回一个rejected promise来跳过此分片上传
- `after-send-file`:在所有分片都上传完毕后，且没有错误后request，用来做分片验证，此时如果promise被reject，当前文件上传会触发错误。

猜测：

这些command是在容器内部就定义好的，应当已经有容器定义好的handler进行一些处理，而我们只是继续注册handler，我们注册的handler的执行在容器定义好的handler之后。