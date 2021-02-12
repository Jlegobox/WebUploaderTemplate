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

// 大文件分片计算逻辑
$(
    function (){
        var inputFile = document.getElementById("file2");
        inputFile.addEventListener('change',function (){
            var blobSlice = blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice;
            var file = this.files[0];
            var chunkSize = 26214400; // 切片大小为1MB(并不是越小越好，应选取适当值)
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