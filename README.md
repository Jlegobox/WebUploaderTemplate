# 基于Web Uploader的文件上传模板
## 实现功能

目前是比较基础的模板，基于Web Uploader组件和Spark-md5组件实现了以下功能：

- 前端大文件MD5计算
- 大文件分片，多线程上传
- 文件秒传功能
- 文件断点续传功能

## 项目架构

本项目分为两个部分：

- Spark-md5使用示例
- 文件上传示例

另在项目中，增加了Web Uploader的介绍文件，相对于官方文件，对于一些基础的部分有更详细的描述，希望可以给有需要的朋友一些帮助。

## Spark-md5使用示例

> Spark-md5是一种前端组件，用于快速计算md5值。MD5是一种信息摘要算法，对于任意的输入都可以转换为128位的散列值。注意，**MD5是有极小概率发生冲突的**，因此要求高的场合不能单纯根据MD5值相同判断输入相同。
>
> 本项目仅是简单的示例，更多细节请参考[Spakr-md5官方文档](https://github.com/satazor/js-spark-md5)

包含两个文件`static/sparkMD5-template.html`和`static/js/sparkMD5Template.js`。

### 引入Spark-md5组件

```html
    <!--jquery-->
    <script type="text/javascript" src="js/jquery-3.5.1.js"></script>
    <!--spark-md5-->
    <script type="text/javascript" src="js/spark-md5.js"></script>

    <script type="text/javascript" src="js/sparkMD5Template.js"></script>
```

其中jquery需要放在最前面，因为是其他组件的前提。计算的逻辑都写在`js/sparkMD5Template.js`中。IDEA需要使用Spark-md5的提醒功能，需要在file->setting->Languages&Frameworks->JavaScript->Libraries中导入库。



### 功能展示

#### 计算输入字符串的MD5值

```js
function calMD5One(){
    // 简单例子
    //计算MD5值
    var inputStr = $("#inputStr").val();
    alert(inputStr)
    // hex hash
    var StrHash = SparkMD5.hash(inputStr);
    $("#MD5OneHex").html(StrHash);
    // raw hash (binary string)
    $("#MD5OneRaw").html(SparkMD5.hash(inputStr,true))
}
```

直接使用`SparkMD5.hash()`就可以直接计算，默认返回的是经过十六位编码的。其中第二个参数可以传入true来获得原生编码。

#### 递增计算

```js
function calMD5Two(){
    var inputStrOne = $("#inputStrOne").val();
    var inputStrTwo = $("#inputStrTwo").val();
    var mergedStr = inputStrOne + inputStrTwo;
    $("#mergedStr").html(mergedStr);
    $("#inputStrOneMD5").html(SparkMD5.hash(inputStrOne));
    $("#inputStrTwoMD5").html(SparkMD5.hash(inputStrTwo));
    $("#mergedStrMD5").html(SparkMD5.hash(mergedStr));
    var spark = new SparkMD5();
    spark.append(inputStrOne);
    spark.append(inputStrTwo);
    $("#mergedStrMD5Two").html(spark.end());
}
```

通过`SparkMD5()`构造函数新建一个`SparkMD5`对象spark，用于分步计算字符串的MD5值。可以通过append()函数顺序的添加字符串，调用end()函数返回输入的字符串的MD5值，且会清空spark对象中的字符串。

#### 计算文件的MD5

```js
function calMD5Three(){
    var file = document.getElementById("file").files[0];
    $("#fileName").html(file.name);
    $("#fileSize").html(file.size/1024/1024/1024 + "GB");
    var startTime = new Date().getTime();
    var spark = new SparkMD5.ArrayBuffer();
    var fileReader = new FileReader();
    fileReader.readAsArrayBuffer(file) // 不使用ArrayBuff会导致文件大小不一致，会存在自动补齐等情况。
    fileReader.onload = function (e){
        spark.append(e.target.result); // 导入二进制形式的文件内容
        $("#fileMD5").html(spark.end());
        var costTime = (new Date().getTime() - startTime)/1000
        $("#costTime").html(costTime.toString())
    }
}
```

计算文件的MD5值：

- 首先要读取文件的二进制内容。这里使用浏览器自带的`FileReader`对象。使用`readAsArrayBuffer()`函数读取二进制文件内容。
- 使用`SparkMD5.ArrayBuffer()`函数创建可以读取ArraBuffer对象的数据。
- 定义`fileReader.onload`事件。

#### 大文件分片计算逻辑

```js
// 大文件分片计算逻辑
$(
    function (){
        var inputFile = document.getElementById("file2");
        inputFile.addEventListener('change',function (){
            var blobSlice = blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice;
            var file = this.files[0];
            var chunkSize = 1048576; // 切片大小为1MB(并不是越小越好，应选取适当值)
            var chunks = Math.ceil(file.size / chunkSize);
            var currentChunk = 0;
            var spark = new SparkMD5.ArrayBuffer();
            var fileReader = new FileReader();

            // 展示
            $("#fileName2").html(file.name);
            $("#fileSize2").html(file.size/1024/1024/1024 + "GB");
            var startTime = new Date().getTime();

            fileReader.onload = function (e){
                console.log('read chunk nr', currentChunk + 1, 'of', chunks);
                spark.append(e.target.result); // 导入二进制形式的文件内容
                currentChunk++;
                if(currentChunk<chunks){
                    loadNext(); // 继续添加后续文件
                }else { // 全部添加完成
                    console.log('finished loading');
                    var MD5 = spark.end();
                    console.info('computed hash', MD5);  // 计算MD5

                    $("#fileMD52").html(MD5);
                    var costTime = (new Date().getTime() - startTime)/1000
                    $("#costTime2").html(costTime.toString())
                }
            }

            fileReader.onerror = function () {
                console.warn('oops, something went wrong.');
            };

            function loadNext() {
                var start = currentChunk * chunkSize;
                var end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;

                fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
            }

            loadNext(); // 开始上传
        })

    }
)
```

大文件文件的MD5值计算主要是由于前端设备性能差，一次性读取整个文件会造成严重卡顿或者根本不能进行读取。所以通过分片的方式进行计算，用到了MD5值可以递增计算的特点。这里主要完成以下步骤：

- 定义分片大小并计算分片的数量
- 定义`FileReader`对象fileReader和`SparkMD5.ArrayBuffer()`对象spark
- 定义分片工具blobSlice，用于对读取的Blob对象进行分片。适用于不同浏览器
- 定义`fileReader.onload`事件，将二进制数据传入到spark中
- 定义loadNext()函数，对传入的file进行分片。

**注意：**这里定义的blobSlice对象只能定义Blob对象而不能操作File对象，尽管File对象继承自Blob对象。如果想操作File对象可以使用以下方式获得切片操作函数并定义`loadNext()`函数。

```js
function blobSlice(blob,startByte,endByte){
    // 使用这个函数，根据传入的blob，可以兼容传入的blob是File类型的情况

    if(blob.slice){
        return  blob.slice(startByte,endByte);
    }
    // 兼容firefox
    if(blob.mozSlice){
        return  blob.mozSlice(startByte,endByte);
    }
    // 兼容webkit
    if(blob.webkitSlice){
        return  blob.webkitSlice(startByte,endByte);
    }
    return null;
}

function loadNext() {
            let start = currentChunk * chunkSize,
                end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;
            fileReader.readAsArrayBuffer(blobSlice(file, start, end));
        }

```



## 文件上传示例

> 文件上传示例使用了Web Uploader上传组件完成上传逻辑，使用MD5作为文件的唯一标识符。Web Uploader的介绍可看[WebUploader使用示例](https://github.com/Jlegobox/WebUploaderTemplate/blob/main/WebUploader使用示例.md)
>
> **注意：**在实际情况中，MD5值可能会有冲突，因此不能单纯靠MD5值来校验文件。

项目分为前端和后端两个部分。前端主要完成文件的分片和上传请求的发送，后端主要负责文件分片的接收和上传结束后，分片的合并保存。在此期间通过MD5值用于验证文件以及文件分片的完整性。

此示例提供的上传逻辑比较简单，根据需要可以适当拓展校验和文件储存的逻辑。为了简化示例，所有的文件仅保存在强制定义文件路径中。可以根据需要修改，或者拓展自定义保存路径的逻辑。但是文件中又增加了过多的MD5验证，可以根据需求进行删减

#### 文件简述

**前端文件**

展示页面：index.html

js文件：uploaderTemplate.js

**后端文件**

controller：UploadController.java 用于请求的接收和返回，调用service进行处理。

service：UploaderService.java 定义了各种处理逻辑

工具类：FileUtil.java 定义了文件处理各项逻辑。 MD5Util.java 后端计算文件MD5的工具类

#### 逻辑简述

前端基于WebUploader组件，将文件上传拆分为多个事件，并且提供了多个钩子函数让我们在一些步骤节点进行一些验证和计算操作。

这里要注意的是，由于前端是多线程的，因此需要注意对于线程不安全量的引用会导致数据的不安全。另外异步操作也可能使得后面步骤发生时，前面步骤还未结束，导致一些错误。这里WebUploader提供了借助Promise对象实现同步的方法。

具体流程如下：

- 文件添加事件：添加文件后，更新前端的文件展示。
- 计算整体的文件的MD5值并向后端请求验证是否已上传。已上传则跳过文件。
- 计算分片的MD5值并向后端请求验证是否已上传。已上传则跳过分片。
- 上传分片。
- 分片上传完成发送合并请求，通知后端进行合并。

前端处理的一些问题

- 分片MD5计算的信息需要在整个流程中流动。因此某分片的信息计算完成后，保存在了次分片对象中，用于流动。

后端处理的一些问题

- 约定了一些指令，例如"permit"，"exists"等来和前端进行交互
- 定义了FileInfo对象用于保存前端传送而来的文件信息。并在FileUtil中定义了根据request创建FileInfo对象的方法。不存在的对象属性保存为默认值。
- 上传的文件分片保存到一个以完整文件MD5值命名的文件夹中。其中还保存了一个`MD5值.INFO`文件。此文件为FileInfo对象的持久化对象，储存了该上传中文件的上传状态和各分片状态。最后合并时会利用保存的分片MD5值进行再次验证。
- 默认的multipart大小最大为1M。根据需求可以再`application.properties`中修改
```properties
# 设置单个request中携带的multipart大小
spring.servlet.multipart.max-file-size = 10485760
spring.servlet.multipart.max-request-size=100MB



