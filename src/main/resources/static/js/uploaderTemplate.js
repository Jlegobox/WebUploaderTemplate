// 相关变量
var uploader; // 保存WebUploader容器
var status; // uploader目前状态
var uploadFileStatus; // 文件状态
var uploadFileMD5; // 文件MD5

function init_uploader(){
    createUploader()
}

function createUploader(){
    // register需要放在new Uploader或者create之前
    // 使用hook接口，返回Promise对象，WebUploader就会监听此异步操作结束
    WebUploader.Uploader.register({
        'before-send-file':'checkFileUploaded', // 分片前进行，此时还未分片
        'before-send':'sendSlice' // 比on("uploadBeforeSend")早
    },{
        checkFileUploaded:function (file){
            console.log("before-send-file")
            var owner = this.owner; // 此uploader对象
            var options = this.options; // webUploader的各种参数
            var deferred = WebUploader.Deferred();
            var uploadFile = file.source.getSource(); // 转换为file对象，file对象继承自blob
            var md5Promise = calMD5(uploadFile) // MD5计算，返回Promise容器

            md5Promise.then(function (md5) { // MD5计算完成
                // MD5服务器校验
                $.ajax({
                    url:"checkUploadFile.ajax",
                    type:"POST",
                    sync:false,
                    data:{
                        "uploadFileMD5":md5
                    },
                    success:function (result){
                        switch (result){
                            case "permit":{
                                // 文件在后台状态，仅记录状态
                                // options是全局文件，多线程情况下可能会造成并发
                                var fileInfo = {};
                                fileInfo["uploadFileMD5"] = md5;
                                fileInfo["uploadFileExists"] = result;
                                file["fileInfo"]= fileInfo;
                                // options["formData"]["uploadFileMD5"] = md5;
                                // options["formData"]["uploadFileExists"] = result;
                                // options["formData"]["fileInfo"] = JSON.stringify(uploadFile);
                                deferred.resolve();// 调用使得webuploader可以继续往下走
                                break;
                            }
                            case "exists":{
                                // 跳过文件
                                deferred.reject();
                                break;
                            }
                        }

                    },
                    error:function (error){
                        console.log(error)
                    }
                })
            })
            return deferred.promise(); //最后需要返回Promise
        },

        sendSlice:function (block){
            // 保证在此分片上传前进行此命令处理且处理完成后才会上传。但是多线程情况会抢占共有属性例如options
            console.log("register before send")
            var options = this.options;
            var deferred = WebUploader.Deferred();
            var uploadFile = block.blob.getSource();
            var fileInfo = {} // 使用新建fileInfo的方式避免引用
            fileInfo["uploadFileMD5"] = block.file["fileInfo"]["uploadFileMD5"];
            fileInfo["uploadFileExists"] = block.file["fileInfo"]["uploadFileExists"];
            var md5Promise = calMD5(uploadFile) // MD5计算，返回Promise容器

            md5Promise.then(function (md5){
                // MD5服务器校验
                $.ajax({
                    url:"checkUploadFileSlice.ajax",
                    type:"POST",
                    sync:false,
                    data:{
                        "uploadFileMD5":fileInfo["uploadFileMD5"],
                        "uploadFileSliceMD5":md5,
                        "chunk":block["chunk"]
                    },
                    success:function (result){
                        switch (result){
                            case "permit":{
                                fileInfo["uploadFileSliceMD5"] = md5;
                                fileInfo["uploadFileSliceExists"] = result;
                                block["fileInfo"] = fileInfo;
                                deferred.resolve();// 调用使得webuploader可以继续往下走
                                break;
                            }
                            case "exists":{
                                deferred.reject();
                                break;
                            }
                        }
                        // options["formData"]["uploadFileSliceMD5"] = md5;
                        // options["formData"]["uploadFileSliceExists"] = result;

                    },
                    error:function (error){
                        console.log(error)
                    }
                })

            })
            return deferred.promise(); //最后需要返回Promise
        }
    })

    // 初始化容器
    uploader = new WebUploader.Uploader({

        // swf文件路径
        swf: '/js/datacloud/Uploader.swf',

        // 文件接收服务端。
        server: 'doUploadFile.ajax',

        formData:{
            // "current_location":current_location
        },

        // 选择文件的按钮。可选。
        // 内部根据当前运行是创建，可能是input元素，也可能是flash.
        pick: '#picker',

        // 不压缩image, 默认如果是jpeg，文件上传前会压缩一把再上传！
        // 默认为false，即压缩
        resize: true,
        //
        chunked:true,
        chunkSize:5242880 //5M大小分片

    });

    // 当有文件被添加进队列时执行
    uploader.on( 'fileQueued', function( file ) {
        // 文件信息展示
        $("#thelist").append( '<div id="' + file.id + '" class="item">' +
            '<h4 class="info">' + file.name + '</h4>' +
            '<p class="state">等待上传...</p>' +
            '</div>' );
    });

    // 文件上传过程中创建进度条实时显示。
    uploader.on('uploadProgress', function( file, percentage ) {
    });


    // 大文件分片上传前设置,在uploadStart之后
    uploader.on('uploadBeforeSend',function (object, data, header){
        console.log("分片上传")
        // 计算分片的MD5值，传输完整性检验

        if(data["uploadFileExists"] === "exists"){
            // 前端显示
            return false;
        }
        if(data["uploadFileSliceExists"] === "exists"){
            // 跳过分片
            return true;
        }
        // 打包分片信息
        var fileInfo = object["fileInfo"];
        data["uploadFileMD5"] = fileInfo["uploadFileMD5"];
        data["uploadFileExists"] = fileInfo["uploadFileExists"]
        data["uploadFileSliceMD5"] = fileInfo["uploadFileSliceMD5"];
        data["uploadFileSliceExists"] = fileInfo["uploadFileSliceExists"]

    })

    // 上传成功时执行
    uploader.on('uploadSuccess', function( file ) {
        console.log("success")
    });

    // 上传失败时执行
    uploader.on('uploadError', function( file ) {
        console.log("error")
    });

    // 上传结束（不论成功失败）时执行
    uploader.on('uploadComplete', function( file ) {
        console.log("complete")
        $.ajax({
            url:"doMergeFile.ajax",
            type:"POST",
            data:{
                "uploadFileMD5":file["fileInfo"]["uploadFileMD5"]
            },
            success:function (result){
                console.log(result)
            },
            error:function (result){
                console.log(result)
            }
        })
    });

    $("#startUpload").on('click', function () {
        uploader.upload();
        // if (state === 'uploading') {
        //     uploader.stop();
        // } else {
        //     uploader.upload();
        //     timer = setInterval(function () {
        //         var useTime = parseInt($("#useTime").html());
        //         useTime = useTime + 1;
        //         $("#useTime").html(useTime);
        //     }, 1000);
        // }
    });
}

function calMD5(file){ // 返回Promise对象
    return new Promise((resolve, reject) => {
        let chunkSize = 5242880,                             // Read in chunks of 5MB
            chunks = Math.ceil(file.size / chunkSize),
            currentChunk = 0,
            spark = new SparkMD5.ArrayBuffer(),
            fileReader = new FileReader();
        fileReader.onload = function (e) {
            console.log('read chunk nr', currentChunk + 1, 'of', chunks);
            spark.append(e.target.result);                   // Append array buffer
            currentChunk++;
            loadProcess(currentChunk/chunks);
            if (currentChunk < chunks) {
                loadNext();
            } else {
                console.log('finished loading');
                let md5 = spark.end();
                console.info('computed hash', md5);  // Compute hash
                resolve(md5);
            }
        };

        fileReader.onerror = function (e) {
            console.warn('oops, something went wrong.');
            reject(e); // 可能的错误地点
        };

        function loadNext() {
            let start = currentChunk * chunkSize,
                end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;
            fileReader.readAsArrayBuffer(blobSlice(file, start, end));
        }

        loadNext();
    });
}

// 上传文件切片,兼容不同的浏览器
// function blobSlice(blob, startByte, endByte) { // 返回的blobSlice.call(file,start,end)中file必须为blob而不能是File
//     return blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice;
// }

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

function loadProcess(width){
    document.getElementById("uploadStatus").innerHTML="文件解析中"+width*100 + "%";
    document.getElementById("uploadBar").setAttribute("lay-percent",width*100 + "%");
}