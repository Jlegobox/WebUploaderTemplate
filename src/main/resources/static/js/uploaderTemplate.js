// 相关变量
var uploader; // 保存WebUploader容器
var status; // uploader目前状态
var uploaderToken; // 上传权限验证
var current_location = 8;

function init_uploader(){
    createUploader()
}

function createUploader(){
    // 初始化容器
    uploader = WebUploader.create({

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
        resize: false
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
    uploader.on( 'uploadProgress', function( file, percentage ) {
        // var $li = $( '#'+file.id ),
        //     $percent = $li.find('.progress .progress-bar');
        //
        // // 避免重复创建
        // if ( !$percent.length ) {
        //     $percent = $('<div class="progress progress-striped active">' +
        //         '<div class="progress-bar" role="progressbar" style="width: 0%">' +
        //         '</div>' +
        //         '</div>').appendTo( $li ).find('.progress-bar');
        // }
        //
        // $li.find('p.state').text('上传中');
        //
        // $percent.css( 'width', percentage * 100 + '%' );
    });

    // 上传前执行，此时还未分片
    uploader.on('uploadStart',function (file){
        alert("上传前")
        // MD5值计算并保存
        // MD5校验
        // getCurrentLocation();
    })

    // 大文件分片上传前设置,在uploadStart之后
    uploader.on('uploadBeforeSend',function (object, data, header){
        header['lg_token'] = uploaderToken
        alert("uploadBeforeSend")
        // 计算分片的MD5值，传输完整性检验
    })

    // 上传成功时执行
    uploader.on('uploadSuccess', function( file ) {
        alert("success")
        // $( '#'+file.id ).find('p.state').text('已上传');
    });

    // 上传失败时执行
    uploader.on('uploadError', function( file ) {
        alert("error")
        // $( '#'+file.id ).find('p.state').text('上传出错');
    });

    // 上传结束（不论成功失败）时执行
    uploader.on('uploadComplete', function( file ) {
        alert("complete")

        // $( '#'+file.id ).find('.progress').fadeOut();
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

function calMD5(file){
    return new Promise((resolve, reject) => {
        let chunkSize = 1048576,                             // Read in chunks of 1MB
            chunks = Math.ceil(file.size / chunkSize),
            currentChunk = 0,
            spark = new SparkMD5.ArrayBuffer(),
            fileReader = new FileReader();
        fileReader.onload = function (e) {
            console.log('read chunk nr', currentChunk + 1, 'of', chunks);
            spark.append(e.target.result);                   // Append array buffer
            currentChunk++;
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
            reject(e);
        };

        function loadNext() {
            let start = currentChunk * chunkSize,
                end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;
            fileReader.readAsArrayBuffer(blobSlice(file.source.getSource(), start, end));
        }

        loadNext();
    });
}

// 上传文件切片
function blobSlice(blob, startByte, endByte) {
    if (blob.slice) {
        return blob.slice(startByte, endByte);
    }
    // 兼容firefox
    if (blob.mozSlice) {
        return blob.mozSlice(startByte, endByte);
    }
    // 兼容webkit
    if (blob.webkitSlice) {
        return blob.webkitSlice(startByte, endByte);
    }
    return null;
}