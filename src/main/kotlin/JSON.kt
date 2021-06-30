import java.lang.StringBuilder

class JSON {
    companion object{
        fun <T> toJSON(list: List<T>, customToString: (T) -> String = {it.toString()}): String{
            val stringBuilder = StringBuilder()
            with(stringBuilder) {
                append("[")

                for(i in list.indices) {
                    append(customToString(list[i]))
                    if(i != list.size-1){
                        append(",")
                    }
                }

                append("]")
            }
            return stringBuilder.toString()
        }
    }
}