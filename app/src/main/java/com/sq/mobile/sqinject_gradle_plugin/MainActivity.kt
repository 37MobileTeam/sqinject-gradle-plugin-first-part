package com.sq.mobile.sqinject_gradle_plugin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * 没有插件之前，使用资源的时候，如下
         * 这样子访问的时候，存在的问题如下：
         * 1）写错一个字（如str_sq写成了str_s），编译的时候不检测，然后运行的时候就会出现问题
         * 2）同时如果修改了字符串的key（如str_sq改成了str_s），那么使用的地方就需要一个个修改，很麻烦
         * */
        resources.getIdentifier(
            "str_sq",
            ResType.TYPE_STRING,
            packageName
        )

        /**
         * 有插件之后，使用资源的时候，如下
         * 这样做的好处：
         * 1）编程的时候，有提示并且编译的时候有检测，方便又安全
         * 2）如果修改字符串的key的时候（如str_sq改成了str_s），只需要跑下插件，重新生成下SqR，
         * 就可以类似原生的R.id那样，愉快的修改了
         * */
        resources.getIdentifier(
            SqR.string.str_sq,
            ResType.TYPE_STRING,
            packageName
        )

    }
}
