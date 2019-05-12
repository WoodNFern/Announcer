package bot.main;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import bot.user.User;
import bot.user.UserManager;

public class AnnouncerBot extends TelegramLongPollingBot {

	private UserManager userManager;
	private MessageSender sender;
	private BotDataProcessor processor;
	public static long SUPER_ADMIN = 0;

	public static boolean blockedSaving = false;

	public static final String USER_FILE = "/data/users.bin";
	public static final String ADMIN_FILE = "/data/admins.bin";
	public static final String CREDENTIALS = "/data/credential.conf";

	private final String botname;
	private final String token;

	public AnnouncerBot(String username, String token, long superAdmin) {
		AnnouncerBot.SUPER_ADMIN = superAdmin;
		userManager = new UserManager();
		sender = new MessageSender(this);
		processor = new BotDataProcessor(this, userManager, sender);
		HashMap<Long, User> users = userManager.load(new File(USER_FILE));
		HashMap<Long, User> admins = userManager.load(new File(ADMIN_FILE));
		this.botname = username;
		this.token = token;

		if (users != null) {
			userManager.setUsers(users);
		}
		if (admins != null) {
			userManager.setAdmins(admins);
		}

		if (blockedSaving) {

			processor.displaySaveConfirm();
		}

	}

	@Override
	public void onUpdateReceived(Update update) {
		if (update.hasMessage()) {

			if (update.getMessage().hasText() && !update.getMessage().hasPhoto()) {
				String message = update.getMessage().getText();
				long chatID = update.getMessage().getChatId();

				if (userManager.isAdmin(chatID)) {
					if(message.toLowerCase().startsWith("/chatid")) {
						processor.showChatID(update);
					}
					if (message.toLowerCase().startsWith("/add")) {
						processor.addInfo(update, message.toLowerCase().replaceAll("/add", ""));
					}
					if (message.toLowerCase().startsWith("/remove")) {
						processor.removeInfo(update, message.toLowerCase().replaceAll("/remove", ""));
					}

					if (message.toLowerCase().startsWith("/removeadmin")) {
						processor.displayRemoveAdmin(update,userManager.getAdmins());
					}
					if (message.startsWith("@")) {
						message = message.substring(1);
						String group = message.split("\n")[0];
						try {
							message = message.substring(group.length() + 1);

							HashSet<User> usersToSend = userManager
									.usersOnList(TelegramList.valueOf(group.toUpperCase()));

							for (User u : usersToSend) {
								sender.sendMessage(message, u.id);
							}
						} catch (IllegalArgumentException e) {
							processor.sendAvailableGroups(update, group);
						} catch (StringIndexOutOfBoundsException e) {
							sender.sendMessage("No text specified", update);
						}

					} else if (message.toLowerCase().startsWith("/groupcount")) {

						message = message.toLowerCase().replaceAll("/groupcount", "");
						message = message.trim();
						try {
						TelegramList list = TelegramList.valueOf(message.toUpperCase());
						int count = userManager.usersOnList(list).size();
						String outputMessage = "There ";
						if (count == 1) {
							outputMessage += "is 1";
							outputMessage += " user in group ";
						} else {
							outputMessage += "are ";
							outputMessage += count;
							outputMessage += " users in group ";
						}

						outputMessage += list;
						sender.sendMessage(outputMessage, update);
						}catch(IllegalArgumentException e) {
							processor.sendAvailableGroups(update, message);
						}
						}

				}

				if (message.toLowerCase().startsWith("/adminrequest")) {
					processor.processAdminRequest(update);
				}

				processor.reactOnSubChange(update, message, "sub");
				processor.reactOnSubChange(update, message, "unsub");
				processor.reactOnInfoRequest(update, message);
				processor.reactOnGroupInfoRequest(update, message);
				processor.reactOnCommandsRequest(update,message);
				processor.sendWelcomeMessage(update, message);
				
			} else if (update.getMessage().hasPhoto()) {

				if (userManager.isAdmin(update.getMessage().getChatId())) {
					processor.sendBroadcastPhoto(update);
				} else {
					sender.sendMessage("You do not have the rights to do that!", update);
				}

			}

		} else if (update.hasCallbackQuery()) {
			processor.buttonHandler(update);
		}

	}

	
	
	@Override
	public String getBotUsername() {

		return botname;
	}

	@Override
	public String getBotToken() {

		return token;
	}

}