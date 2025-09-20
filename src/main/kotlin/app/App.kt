package org.example

import app.GoogleSheetsClient
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.KeyboardButton
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import io.github.cdimascio.dotenv.dotenv
import org.example.app.ApplicationForm
import org.example.app.FormState
import org.example.app.Session
import java.time.ZoneId
import java.time.ZonedDateTime

fun main() {
    val env = dotenv { ignoreIfMissing = false }
    val botToken = env["TELEGRAM_BOT_TOKEN"]
    val spreadsheetsId = env["SPREADSHEET_ID"]
    val sheetName = env["SHEET_NAME"]
    val credsPath = env["GOOGLE_CREDENTIALS_JSON"]
    val adminHandle = env["ADMIN_HADLE"] ?: "@Emma_Shleyger"
    val adminPhone = env["ADMIN_PHONE"] ?: "+7-999-794-59-57"

    val sheets = GoogleSheetsClient (credsPath, spreadsheetsId)
    val bot = TelegramBot(botToken)

    val sessions = mutableMapOf<Long, Session>()

    val menu = ReplyKeyboardMarkup(
        arrayOf(KeyboardButton("Подать заявку")),
        arrayOf(KeyboardButton("Помощь"))
    ).resizeKeyboard(true).oneTimeKeyboard(false)


fun startMessage(chatId: Long) {
    val text = """
        Привет! Я бот для приема заявок мастеров.
        Заявку возможно подать 2 способами:
        1) Нажать "Подать заявку" и пройти пошаговый ввод.
        2) Отправить одной строкой по форме: Имя мастера; Район города; Услуга; Цена в рублях.
        """.trimIndent()
    bot.execute(SendMessage(chatId, text).replyMarkup(menu))
}

    fun helpMessage(chatId: Long){
        val text = """
            Помощь:
            /start - начать заново
            /help - список команд
            /cancel - отменить ввод
            Администратор: $adminHandle
            Телефон: $adminPhone
            """.trimIndent()
        bot.execute(SendMessage(chatId, text).replyMarkup(menu))
    }

    fun cancel (chatId: Long){
        val text = "Вы отменили заполнение заявки!"
        sessions.remove(chatId)
        bot.execute(SendMessage(chatId, text).replyMarkup(menu))
    }

    fun parseOneLine (raw: String): ApplicationForm?{
        val parts = raw.split(";").map { it.trim() }
        if (parts.size !=4) return null
        val price = parts [3].replace(",",".").toBigDecimalOrNull() ?: return null
        return ApplicationForm(parts[0], parts[1], parts[2], price)
    }

    fun finalizeAndSave (chatId: Long, message: Message, form: ApplicationForm){
        val username = message.from()?.username() ?: ""
        val timestamp = ZonedDateTime.now(ZoneId.of("Europe/Moscow")).toString()
        val row = listOf(
            timestamp,
            form.name ?: "",
            form.district ?: "",
            form.service ?: "",
            form.price?.toPlainString() ?: "",
            username.ifBlank { "" },
            chatId.toString()
        )
        sheets.appendRow(sheetName, row)
        bot.execute(SendMessage(chatId, "✅ Заявка записана в таблицу!\nИмя: ${form.name}\nРайон: ${form.district}\nУслуга: ${form.service}\nЦена: ${form.price}").replyMarkup(menu))
        sessions.remove(chatId)
    }


    fun handleStep (chatId: Long, message: Message, text: String){
        val session = sessions.getOrPut(chatId) { Session() }
        when (session.state){
            FormState.IDLE -> {
                session.state = FormState.WAITING_NAME
                bot.execute(SendMessage(chatId, "Шаг 1. Введите имя мастера."))
            }
            FormState.WAITING_NAME -> {
                session.state = FormState.WAITING_DISTRICT
                bot.execute(SendMessage(chatId, "Шаг 2. Введите район оказания услуги."))
            }

            FormState.WAITING_DISTRICT -> {
                session.state = FormState.WAITING_SERVICE
                bot.execute(SendMessage(chatId, "Шаг 3. Введите наименование услуги."))
            }

            FormState.WAITING_SERVICE -> {
                session.state = FormState.WAITING_PRICE
                bot.execute(SendMessage(chatId, "Шаг 4. Введите цену услуги числом."))
            }

            FormState.WAITING_PRICE -> {
                val price = text.replace(",", ".").toBigDecimalOrNull()
                if (price == null){
                bot.execute(SendMessage(chatId, "Цена должна быть числом, примерЖ 1500"))
            } else {
                session.form.price = price
                    finalizeAndSave(chatId,message,session.form)
                }
        }
    }
}
    bot.setUpdatesListener { updates ->
        for (u in updates) {
            val msg = u.message() ?: continue
            val chatId = msg.chat().id()
            val text = msg.text() ?: ""

            when {
                text.equals("/start", true) -> startMessage(chatId)
                text.equals("/help", true) || text.equals("Помощь", true) -> helpMessage(chatId)
                text.equals("/cancel", true) -> cancel(chatId)
                text.equals("Подать заявку", true) -> handleStep(chatId, msg, text)
                else -> {
                    val parsed = parseOneLine(text)
                    if (parsed != null) {
                        finalizeAndSave(chatId, msg, parsed)
                    } else {
                        val state = sessions[chatId]?.state ?: FormState.IDLE
                        if (state == FormState.IDLE) {
                            bot.execute(SendMessage(chatId, "Можно прислать по шаблону: Имя; Район; Услуга; Цена\nили нажать «Подать заявку»").replyMarkup(menu))
                        } else {
                            handleStep(chatId, msg, text)
                        }
                    }
                }
            }
        }
        UpdatesListener.CONFIRMED_UPDATES_ALL
    }

    println("🚀 Bot started")
}