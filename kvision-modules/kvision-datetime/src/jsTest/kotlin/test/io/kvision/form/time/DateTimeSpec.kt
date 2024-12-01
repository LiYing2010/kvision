/*
 * Copyright (c) 2017-present Robert Jaros
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package test.io.kvision.form.time

import io.kvision.form.Form
import io.kvision.form.time.DateTime
import io.kvision.panel.Root
import io.kvision.test.DomSpec
import io.kvision.types.toStringF
import kotlinx.browser.document
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.js.Date
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class DataForm(
    @Contextual val a: Date? = null
)

class DateTimeSpec : DomSpec {

    @Test
    fun render() {
        run {
            val root = Root("test", containerType = io.kvision.panel.ContainerType.FIXED)
            val data = Date()
            val ti = DateTime(value = data, label = "Label").apply {
                placeholder = "place"
                name = "name"
                disabled = true
            }
            root.add(ti)
            val element = document.getElementById("test")
            val id = ti.input.id
            val datastr = data.toStringF(ti.format)
            assertEqualsHtml(
                "<div class=\"form-group kv-mb-3\"><label class=\"form-label\" for=\"${id}_input\">Label</label><div class=\"input-group date\" data-td-target-input=\"nearest\" data-td-target-toggle=\"nearest\" id=\"$id\"><input class=\"form-control\" data-td-target=\"#$id\" id=\"${id}_input\" placeholder=\"place\" name=\"name\" disabled=\"disabled\" type=\"text\" value=\"$datastr\"><span class=\"input-group-text\" data-td-target=\"#$id\" data-td-toggle=\"datetimepicker\"><span class=\"fas fa-calendar-alt\"></span></span></div></div>",
                element?.innerHTML,
                "Should render correct date time input form control"
            )
            ti.validatorError = "Validation Error"
            assertEqualsHtml(
                "<div class=\"form-group kv-mb-3 kv-text-danger\"><label class=\"form-label\" for=\"${id}_input\">Label</label><div class=\"input-group date is-invalid\" data-td-target-input=\"nearest\" data-td-target-toggle=\"nearest\" id=\"$id\"><input class=\"form-control is-invalid\" data-td-target=\"#$id\" id=\"${id}_input\" placeholder=\"place\" name=\"name\" disabled=\"disabled\" type=\"text\" value=\"$datastr\"><span class=\"input-group-text\" data-td-target=\"#$id\" data-td-toggle=\"datetimepicker\"><span class=\"fas fa-calendar-alt\"></span></span></div><div class=\"invalid-feedback\">Validation Error</div></div>",
                element?.innerHTML,
                "Should render correct date time input form control with validation error"
            )
        }
    }

    @Test
    fun workInForm() {
        run {
            val form = Form.create<DataForm>()
            val now = Date()
            val data = DataForm(a = now)
            form.setData(data)
            val result = form.getData()
            assertEquals(
                now.toStringF("YYYY-MM-DD HH:mm"),
                result.a?.toStringF("YYYY-MM-DD HH:mm"),
                "Form should return initial value without a datetime control"
            )
            val dateTimeField = DateTime()
            form.add(DataForm::a, dateTimeField)
            form.setData(data)
            val result2 = form.getData()
            assertEquals(
                now.toStringF("YYYY-MM-DD HH:mm"),
                result2.a?.toStringF("YYYY-MM-DD HH:mm"),
                "Form should return initial value with a datetime control"
            )
        }
    }

}
