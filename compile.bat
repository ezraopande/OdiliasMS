@echo off
javac -d target\classes --module-path target\libs;target\libs\* src\main\java\module-info.java src\main\java\com\odilias\odms\*.java src\main\java\com\odilias\odms\controller\*.java src\main\java\com\odilias\odms\dao\*.java src\main\java\com\odilias\odms\model\*.java src\main\java\com\odilias\odms\util\*.java
