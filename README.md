
开发环境硬件要求：

1. Windows 64-bit 操作系统的服务器或主机一台；

2. CPU i5以上，i7更好，如果不是，尽量选择计算性能较好的CPU；

3. 内存 16GB 如不能满足，至少8GB

4. 硬盘容量：500GB 视项目数据而定，一般服务器和台式机够用

5. 网络：安装支持离线安装，如果能联网更好

6. 浏览器：如有自带IE，请尽量能安装上类似google chrome的现代浏览器


软件项目源数据要求：

1. 软件源代码：即针对一个领域的某一应用的开发源文件代码（要求是java代码，可以将整个项目打包，但里面必须包含java代码的src）

2. 软件开发文档：对应1中软件开发源代码的所有相关文档，越丰富越好，格式是docx格式，暂不支持其他格式文档；

3. 软件开发版本记录：如果是按照git进行项目管理，可提供项目目录下的.git数据，此项数据为可选，非必备；

4. 软件开发缺陷记录：如果按照jira进行项目缺陷管理，可提供项目的缺陷报告记录，此项数据为可选，非必备；

注：为了达到软件项目知识图谱的演示效果，以上的数据尽可能是公司在某一领域的某一具体应用的完整数据，数据规模不做要求，但尽可能规整，丰富；
    
    如果可以，请尽可能多准备一些项目的原始数据，即不同领域不同应用的开发项目，我们支持多个项目同时进行软件项目知识图谱的构建！

注：以上数据都是绝对安全，不会有任何外网访问的操作，可以确保软件数据的安全可靠。


安装过程：

1. 将后台程序jar包和配置文件拷贝到电脑上，其中，config文件夹一定保存在C盘根目录下！！！

2. 将前端程序snowview-intellide-graph压缩包拷贝到电脑上，解压即可；

3. 将docx_to_html.py脚本和program.xml配置文件样本拷贝到电脑上；

4. 将软件离线安装包拷贝到电脑上，如果有网络，可自行下载；

5. 安装jdk1.8版本，安装python36，安装node10，安装git，安装google chrome，安装解压缩工具；注意，版本要求，jdk 1.8 python 2.6 node 至少8.0以上版本；

6. 根据项目数据路径配置项目配置文件program.xml，并保存在某一目录下；

7. 如果有docx文档数据，运行docx_to_html.py脚本，修改脚本里面的path和html路径，运行命令：

   python docx_to_html文件路径

8. 运行jar包里面的intellide-graph-jar，在当前目录下，运行命令：

   java -Dfile.encoding=utf-8 -jar intellide-graph.jar "path:XX:\program.xml"

9. 修改jar包里面intellide-graph-jar2里面的application.properties文件里面的配置路径，改成第6步生成的图数据库路径和Json文件路径；

10. 运行jar包里面的intellide-graph-jar2，在当前目录下，运行命令：

   java -Xms1024m -Xmx4096m -XX:MaxPermSize=2048m -XX:MaxNewSize=2048m -Dfile.encoding=utf-8 -jar intellide-graph.jar

   运行内存视机器而定，越大越好！

11. 修改snowview-intellide-graph里面的src文件夹下的config文件，将url路径改成运行jar包的主机ip；

12. 在snowview-intellide-graph文件目录下，右键运行git bash，输入 npm install ；然后输入npm start启动前端；

13. 打开google chrome ，访问localhost:3000 即可操作知识图谱；