import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.*;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;

public class iConomy extends Plugin {
	protected static final Logger log = Logger.getLogger("Minecraft");
	private Listener l = new Listener(this);

	// Debugging
	static boolean debugging = true;

	// Directory
	private String directory = "iConomy/", lDirectory = "logs/";

	// Property Files
	private iProperty settings, prizes, buying, selling, auctions, auctioner, itemNames, sign, signOwners, signLocation, trades;

	// Hashmaps for lottery
	private Map<String, String> hashPrizes;

	// Hashmaps for items
	BidiMap items = new TreeBidiMap();

	// Data Control
	public iData data;

	// Ranking
	static HashMap<String, Long> lastClick = new HashMap<String, Long>();

	// Money Timer Settings
	private Timer mTime1, mTime2;
	private int moneyGive = 0;
	private int moneyGiveInterval = 0;
	private int moneyTake = 0;
	private int moneyTakeInterval = 0;

	// Money Settings
	public String moneyName;
	public int startingBalance;

	// Permissions
	public String canPay, canCredit, canDebit, canReset, canRank, canTop, canView, canSell, canBuy, canAuction, canBid, canEnd, canLottery, canSign, canSignSell, canSignBuy;
	public String canTrade;

	// Shop Details
	public boolean globalAuction, globalShop, physicalShop, globalLottery, globalSigns, globalStock;

	// Auction settings
	public Timer auctionTimer = new Timer();
	public String auctionName, auctionStarter, auctionCurName;
	public boolean auctionTimerRunning, auctionReserveMet = false;
	public int auctionStartingBid = 0, auctionInterval, auctionItem = 0, auctionAmount = 0, auctionMin = 0;
	public int auctionReserve = 0, auctionCurAmount = 0, auctionCurBid = 0, auctionCurBidCount = 0, auctionCurSecretBid = 0;

	// Buying Template
	public String buyInvalidAmount, buyNotEnough, buyReject, buyGive;

	// Selling Template
	public String sellInvalidAmount, sellReject, sellGiveAmount, sellGive,  sellNone;

	// Lottery Template
	public String lotteryNotEnough, lotteryLoser, lotteryNotAvailable, lotteryWinner, lotteryLimit, lotteryCost;
	public String lotteryTag = Colors.White + "["+Colors.Green+"Lottery"+Colors.White+"]";

	// Sign Shop Template
	public String signFailedToParse, signAmountGreaterThan, signValuesNotNumerical, signInvalidItem, signMaxBuySell, signTransactionGreaterThan, signSellCreated, signBuyCreated;
	private String signWait, signOwnerBankrupt, signUpgradeAmount, signUpgradeExists, signSold, signStocked, signNotEnough, signNotInStock,signBoughtAmount,signLeftAmount, signNotEnoughp, signInvalid;
	private String signNotInStocky,signNoExists;

	// Logging
	public boolean logPay, logBuy, logSell, logAuction;

	// Tickets
	public int ticketCost;

	// Sign Shop Data
	private boolean signOwnUse;
	private int signOwnAmount, signOwnUpgrade, signMaxAmount, signWaitAmount;

	// Database
	private boolean mysql;
	private String driver, user, pass, db;

	// Versioning
	private String   version = "0.9.4.7";
	private String  sversion = "0.8.5";
	private String  aversion = "0.5";
	private String  lversion = "0.3";
	private String ssversion = "0.3a";
	private String  tversion = "0.1a";

	public iConomy() {
		this.settings = null;
		this.data = null;
		this.mTime1 = null;
		this.mTime2 = null;
		this.auctionTimer = null;
	}

	public void enable() {
		if (load()) {
			log.info("[iConomy v" + this.version + "] Plugin Enabled.");

			if(debugging)
				log.info("[iConomy Debugging] Enabled.");

			String latest = getVersion();
			if(latest != null) {
				if(!latest.equals(version)) {
					log.info("[iConomy Update] A new version of iConomy has been released! v"+latest+"!");
				}
			}
		} else {
			log.info("[iConomy v" + this.version + "] Plugin failed to load.");
		}

		etc.getInstance().addCommand("/money", "? - For more information");

		if(this.globalShop)
			etc.getInstance().addCommand("/shop", "? - For more information");

		if(this.globalAuction)
			etc.getInstance().addCommand("/auction", "? - For more information");

		if(this.globalLottery)
			etc.getInstance().addCommand("/lottery", "- Test your luck on the lottery!");

		if(this.globalSigns)
			etc.getInstance().addCommand("/sign", "? - For more information");
	}

	public void initialize() {
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, l, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, l, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, l, this, PluginListener.Priority.HIGH);
		etc.getLoader().addListener( PluginLoader.Hook.BLOCK_DESTROYED, l, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.COMPLEX_BLOCK_CHANGE, l, this, PluginListener.Priority.MEDIUM);
	}

	public void disable() {
		etc.getInstance().removeCommand("/money");

		if(this.globalShop)
			etc.getInstance().removeCommand("/shop");

		if(this.globalAuction)
			etc.getInstance().removeCommand("/auction");

		if(this.globalLottery)
			etc.getInstance().removeCommand("/lottery");

		if(this.globalSigns)
			etc.getInstance().removeCommand("/sign");

		if (this.mTime1 != null)
			this.mTime1.cancel();

		if (this.mTime2 != null)
			this.mTime2.cancel();

		if (this.auctionTimer != null)
			this.auctionTimer.cancel();

		log.info("[iConomy v" + this.version + "] Plugin Disabled.");
	}

	private void halp(Player player, String type) {
		if (type.equals("money")) {
			player.sendMessage(Colors.Rose + "iConomy [Money] v" + this.version + " - by Nijikokun");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "<p> = player, <a> = amount");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/money - Shows your balance");

			if (this.can(player, "view")) {
				player.sendMessage(Colors.Rose + "/money <p> - Shows player balance");
			}

			if (this.can(player, "pay")) {
				player.sendMessage(Colors.Rose + "/money pay <p> <a> - Pay a player money");
			}

			if (this.can(player, "credit")) {
				player.sendMessage(Colors.Rose + "/money credit <p> <a> - Give a player money");
			}

			if (this.can(player, "debit")) {
				player.sendMessage(Colors.Rose + "/money debit <p> <a> - Take a players money");
			}

			if (this.can(player, "rank")) {
				player.sendMessage(Colors.Rose + "/money rank <p> - Show your rank or another players");
			}

			if (this.can(player, "reset")) {
				player.sendMessage(Colors.Rose + "/money reset <p> - Reset a players account balance.");
			}

			if (this.can(player, "top")) {
				player.sendMessage(Colors.Rose + "/money top - Shows top 5");
				player.sendMessage(Colors.Rose + "/money top <a> - Shows top <a> richest players");
			}

			player.sendMessage(Colors.Rose + "/money help|? - Displays this.");
		} else if (type.equals("shop")) {
			player.sendMessage(Colors.Rose + "iConomy [Shop] v" + this.sversion + " - by Nijikokun");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "<i> = item, <a> = amount, [] = optional");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/shop <i> - Shows amount per item for sell/buy");
			player.sendMessage(Colors.Rose + "/shop <i> <a> - Shows amount per <a> for sell/buy");
			player.sendMessage(Colors.Rose + "/shop stock [page #] - Shows stock");
			player.sendMessage(Colors.Rose + "/shop list buy [page #] - Shows buying list");
			player.sendMessage(Colors.Rose + "/shop list sell [page #] - Shows selling list");

			if (this.can(player, "buy")) {
				player.sendMessage(Colors.Rose + "/shop buy <i> - Purchase 1 item");
				player.sendMessage(Colors.Rose + "/shop buy <i> <a> - Purchase multiple items");
			}

			if (this.can(player, "sell")) {
				player.sendMessage(Colors.Rose + "/shop sell <i> - Sell 1 item");
				player.sendMessage(Colors.Rose + "/shop sell <i> <a> - Sell multiple items");
			}

			player.sendMessage(Colors.Rose + "/shop help|? - Displays this.");
		} else if (type.equals("auction")) {
			player.sendMessage(Colors.Rose + "iConomy [Auction] v" + this.aversion + " - by Nijikokun");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "<i> Item, <a> Amount, <s> Secret bid");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/auction - Shows current auction details or auction running information.");

			if(this.can(player, "auction"))
				player.sendMessage(Colors.Rose + "/auction start <time-seconds> <item> <amount> <start-bid>");
				player.sendMessage(Colors.Rose + "    Optional after <start-bid>: [min-bid] [max-bid]");
				player.sendMessage(Colors.Rose + "    Desc: Starts the auction with name for concurrent bids");

			if(this.can(player, "bid"))
				player.sendMessage(Colors.Rose + "/auction bid <a> - bid on the current auction");
				player.sendMessage(Colors.Rose + "/auction bid <a> <s> - bid with a secret amount");

			player.sendMessage(Colors.Rose + "/auction end - ends the current auction");
			player.sendMessage(Colors.Rose + "/auction ? - help documentation");
		} else if (type.equals("lottery")) {
			player.sendMessage(Colors.Rose + "iConomy [Lottery] v" + this.lversion + " - by Nijikokun");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/lottery - Try your luck at winning the lottery!");
			player.sendMessage(Colors.Rose + "/lottery ? - help documentation");
		} else if (type.equals("sign")) {
			player.sendMessage(Colors.Rose + "iConomy [Sign Shop] v" + this.ssversion + " - by Nijikokun & Graniz");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/sign - Check your sign stock");
			player.sendMessage(Colors.Rose + "/sign stock <item> <amount> - Add stock to an item");
			player.sendMessage(Colors.Rose + "/sign empty <item> - Empty sign stock for item");
			player.sendMessage(Colors.Rose + "/sign ? - help documentation");
		}
	}

	/**
	 * Update user state in plugin
	 */
	public void updateState(Player player, boolean write) {
		// Depricated
	}

	private boolean load() {
		// Create directory if it doesn't exist.
		(new File(directory)).mkdir();
		(new File(directory + lDirectory)).mkdir();

		// File Data
		this.settings = new iProperty(directory + "settings.properties");

		// Switches
		this.mysql = this.settings.getBoolean("use-mysql", false);
		this.globalShop = this.settings.getBoolean("use-shop", true);
		this.globalStock = this.settings.getBoolean("use-stock", true);
		this.globalAuction = this.settings.getBoolean("use-auction", true);
		this.globalLottery = this.settings.getBoolean("use-lottery", true);
		this.globalSigns = this.settings.getBoolean("use-signs", true);

		// Debugging
		debugging = this.settings.getBoolean("debugging", false);

		// Money Starting Balance
		this.startingBalance = this.settings.getInt("starting-balance", 0);

		// Lottery Ticket Cost
		this.ticketCost = this.settings.getInt("ticket-cost", 150);

		// Ticker Amounts
		this.moneyGive = this.settings.getInt("money-give", 0);
		this.moneyTake = this.settings.getInt("money-take", 0);

		// Ticker Intervals
		this.moneyGiveInterval = (1000 * this.settings.getInt("money-give-interval", 0));
		this.moneyTakeInterval = (1000 * this.settings.getInt("money-take-interval", 0));

		// Money Name
		this.moneyName = (" " + this.settings.getString("money-name", "coin"));

		// Sign Shop
		this.signMaxAmount = this.settings.getInt("sign-max-amount", 1000);
		this.signOwnUse = this.settings.getBoolean("sign-own-use",true);
		this.signOwnAmount = this.settings.getInt("sign-own-amount", 3);
		this.signOwnUpgrade = this.settings.getInt("sign-upgrade-cost", 300);
		this.signWaitAmount = this.settings.getInt("sign-wait-amount", 2);

		// Groups per Command
		this.canPay = this.settings.getString("can-pay", "*");
		this.canDebit = this.settings.getString("can-debit", "admins,");
		this.canCredit = this.settings.getString("can-credit", "admins,");
		this.canReset = this.settings.getString("can-reset", "admins,");
		this.canRank = this.settings.getString("can-rank", "*");
		this.canTop = this.settings.getString("can-top", "*");
		this.canView = this.settings.getString("can-view-player-balance", "*");
		this.canBuy = this.settings.getString("can-buy", "*");
		this.canSell = this.settings.getString("can-sell", "*");
		this.canAuction = this.settings.getString("can-auction", "*");
		this.canBid = this.settings.getString("can-bid", "*");
		this.canEnd = this.settings.getString("can-end", "admins,");
		this.canLottery = this.settings.getString("can-lottery", "*");
		this.canSign = this.settings.getString("can-make-sign-shops", "*");
		this.canSignBuy = this.settings.getString("can-sign-shop-buy", "*");
		this.canSignSell = this.settings.getString("can-sign-shop-sell", "*");
		this.canTrade = this.settings.getString("can-trade", "*");

		// Buying / Selling logging
		this.logPay = this.settings.getBoolean("log-pay", false);
		this.logBuy = this.settings.getBoolean("log-buy", false);
		this.logSell = this.settings.getBoolean("log-sell", false);
		this.logAuction = this.settings.getBoolean("log-auction", false);

		// Buy / Sell messages
		this.sellGive = this.settings.getString("sell-success", "Your account has been credited with %s!");
		this.sellGiveAmount = this.settings.getString("sell-success-sold", "Sold %d out of %d!");
		this.sellReject = this.settings.getString("sell-rejected", "Sorry, that item is currently unavailable!");
		this.sellNone = this.settings.getString("sell-none", "Whoops, you seem to not have any of that item!");
		this.buyGive = this.settings.getString("buy-success", "Your purchase cost you %s! Here you go :)!");
		this.buyReject = this.settings.getString("buy-rejected", "Sorry, that item is currently unavailable!");
		this.buyNotEnough = this.settings.getString("buy-not-enough", "Sorry, you currently don't have enough to buy that!");
		this.buyInvalidAmount = this.settings.getString("buy-invalid-amount", "Sorry, you must buy these in increments of %d!");
		this.sellInvalidAmount = this.settings.getString("sell-invalid-amount", "Sorry, you must sell these in increments of %d!");

		// Lottery messages
		this.lotteryNotEnough = this.settings.getString("lottery-not-enough", "Sorry, you do not have enough to buy a ticket!");
		this.lotteryWinner = this.settings.getString("lottery-winner", "Congratulations %s won %d %s in the lottery!");
		this.lotteryLoser = this.settings.getString("lottery-loser", "The lady looks at you and shakes her head. Try Again!.");
		this.lotteryNotAvailable = this.settings.getString("lottery-not-available", "Lottery seems to be unavailable this time. Try again!");
		this.lotteryCost = this.settings.getString("lottery-cost", "The lady snatches %s from your hand and gives you a ticket.");

		// Sign Shop messages
		this.signFailedToParse = this.settings.getString("sign-failed-parse","Failed to parse: %s");
		this.signAmountGreaterThan = this.settings.getString("sign-amount-greater-than","Amount must be greater then %d");
		this.signTransactionGreaterThan = this.settings.getString("sign-transaction-greater-than","Transaction amount must be greater then %d");
		this.signValuesNotNumerical = this.settings.getString("sign-values-numerical","Values are not numerical: %s");
		this.signMaxBuySell = this.settings.getString("sign-max-buysell","You may not sell or buy items in amounts over %d");
		this.signInvalidItem = this.settings.getString("sign-invalid-item","Invalid item!");
		this.signWait = this.settings.getString("sign-wait","You need to wait %d seconds before shopping!");
		this.signOwnerBankrupt = this.settings.getString("sign-owner-bankrupt","%s is currently out of %s.");
		this.signSellCreated = this.settings.getString("sign-sell-created","Shop sign created! People can sell %d %s for %s to you.");
		this.signBuyCreated = this.settings.getString("sign-buy-created","Shop sign created! People can buy %d %s for %s from you.");
		this.signUpgradeAmount = this.settings.getString("sign-upgrade-amount","Cannot upgrade! You must have at least %s to upgrade.");
		this.signUpgradeExists = this.settings.getString("sign-upgrade-exists","Sorry! Cannot upgrade, you must create a sign first!");
		this.signSold = this.settings.getString("sign-player-sold","You sold %d %s giving you %s");
		this.signStocked = this.settings.getString("sign-owner-stock", "%d %s has been put into stock!");
		this.signNotEnough = this.settings.getString("sign-owner-not-enough-stock", "%s doesn't have enough %s left in stock!");
		this.signNotInStock = this.settings.getString("sign-owner-no-stock", "%s doesn't have that in stock!");
		this.signNotInStocky = this.settings.getString("sign-you-no-stock", "%s don't have that in stock!");
		this.signBoughtAmount = this.settings.getString("sign-bought-amount","You bought %d of %s, it cost you %s");
		this.signLeftAmount = this.settings.getString("sign-amount-left", "You now have %d left of %s in your stock.");
		this.signNotEnoughp = this.settings.getString("sign-not-enough-player", "You don't have enough %s to do that!");
		this.signInvalid = this.settings.getString("sign-invalid", "Shop no longer exists, resetting text!");
		this.signNoExists = this.settings.getString("sign-non-existant", "You don't have a sign with that item.");

		// To be added
		//this.signUpgraded = this.settings.getString("sign-upgraded", "Successfully upgraded sign shop! You can now use %s signs");

		// Shop Data
		this.auctions = new iProperty(directory + "auction.properties");
		this.auctioner = new iProperty(directory + "auctioner.properties");
		this.trades = new iProperty(directory + "trading.properties");

		if(!this.mysql) {
			this.buying = new iProperty(directory + "buying.properties");
			this.selling = new iProperty(directory + "selling.properties");
			this.prizes = new iProperty(directory + "prizes.properties");
			this.signOwners = new iProperty(directory + "sign-owners.properties");
			this.sign = new iProperty(directory + "signs.properties");
			this.signLocation = new iProperty(directory + "sign-locations.properties");

			try {
				this.hashPrizes = this.prizes.returnMap();
			} catch (Exception ex) {
				log.severe("[iConomy Lottery] Error: " + ex);
			}
		}

		// MySQL
		this.driver = this.settings.getString("driver", "com.mysql.jdbc.Driver");
		this.user = this.settings.getString("user", "root");
		this.pass = this.settings.getString("pass", "root");
		this.db = this.settings.getString("db", "jdbc:mysql://localhost:3306/minecraft");

		// Data
		this.data = new iData(this.mysql, this.startingBalance, this.driver, this.user, this.pass, this.db);

		// Buying
		items.put("1", "stone");
		items.put("2", "grass");
		items.put("3", "dirt");
		items.put("4", "cobblestone");
		items.put("5", "wood");
		items.put("6", "sapling");
		items.put("7", "bedrock");
		items.put("8", "water");
		items.put("9", "still-water");
		items.put("10", "lava");
		items.put("11", "still-lava");
		items.put("12", "sand");
		items.put("13", "gravel");
		items.put("14", "gold-ore");
		items.put("15", "iron-ore");
		items.put("16", "coal-ore");
		items.put("17", "log");
		items.put("18", "leaves");
		items.put("19", "sponge");
		items.put("20", "glass");
		items.put("35", "gray-cloth");
		items.put("37", "yellow-flower");
		items.put("38", "red-rose");
		items.put("39", "brown-mushroom");
		items.put("40", "red-mushroom");
		items.put("41", "gold-block");
		items.put("42", "iron-block");
		items.put("43", "double-step");
		items.put("44", "step");
		items.put("45", "brick");
		items.put("46", "tnt");
		items.put("47", "bookcase");
		items.put("48", "mossy-cobblestone");
		items.put("49", "obsidian");
		items.put("50", "torch");
		items.put("51", "fire");
		items.put("52", "mob-spawner");
		items.put("53", "wooden-stairs");
		items.put("54", "chest");
		items.put("55", "redstone-wire");
		items.put("56", "diamond-ore");
		items.put("57", "diamond-block");
		items.put("58", "workbench");
		items.put("59", "crops");
		items.put("60", "soil");
		items.put("61", "furnace");
		items.put("62", "burning-furnace");
		items.put("63", "sign-post");
		items.put("64", "wooden-door");
		items.put("65", "ladder");
		items.put("66", "mine-cart-tracks");
		items.put("67", "cobblestone-stairs");
		items.put("68", "wall-sign");
		items.put("69", "lever");
		items.put("70", "stone-pressure-plate");
		items.put("71", "iron-door");
		items.put("72", "wooden-pressure-plate");
		items.put("73", "redstone-ore");
		items.put("74", "glowing-redstone-ore");
		items.put("75", "redstone-torch-off");
		items.put("76", "redstone-torch-on");
		items.put("77", "stone-button");
		items.put("78", "snow");
		items.put("79", "ice");
		items.put("80", "snow-block");
		items.put("81", "cactus");
		items.put("82", "clay");
		items.put("83", "reed");
		items.put("84", "jukebox");
		items.put("85", "fence");
		items.put("86", "pumpkin");
		items.put("87", "red-mossy-cobblestone");
		items.put("88", "mud");
		items.put("89", "brittle-gold");
		items.put("90", "portal");
		items.put("91", "jack-o-lantern");
		items.put("256", "iron-spade");
		items.put("257", "iron-pickaxe");
		items.put("258", "iron-axe");
		items.put("259", "steel-and-flint");
		items.put("260", "apple");
		items.put("261", "bow");
		items.put("262", "arrow");
		items.put("263", "coal");
		items.put("264", "diamond");
		items.put("265", "iron-ingot");
		items.put("266", "gold-ingot");
		items.put("267", "iron-sword");
		items.put("268", "wooden-sword");
		items.put("269", "wooden-spade");
		items.put("270", "wooden-pickaxe");
		items.put("271", "wooden-axe");
		items.put("272", "stone-sword");
		items.put("273", "stone-spade");
		items.put("274", "stone-pickaxe");
		items.put("275", "stone-axe");
		items.put("276", "diamond-sword");
		items.put("277", "diamond-spade");
		items.put("278", "diamond-pickaxe");
		items.put("279", "diamond-axe");
		items.put("280", "stick");
		items.put("281", "bowl");
		items.put("282", "mushroom-soup");
		items.put("283", "gold-sword");
		items.put("284", "gold-spade");
		items.put("285", "gold-pickaxe");
		items.put("286", "gold-axe");
		items.put("287", "string");
		items.put("288", "feather");
		items.put("289", "gunpowder");
		items.put("290", "wooden-hoe");
		items.put("291", "stone-hoe");
		items.put("292", "iron-hoe");
		items.put("293", "diamond-hoe");
		items.put("294", "gold-hoe");
		items.put("295", "seeds");
		items.put("296", "wheat");
		items.put("297", "bread");
		items.put("298", "leather-helmet");
		items.put("299", "leather-chestplate");
		items.put("300", "leather-pants");
		items.put("301", "leather-boots");
		items.put("302", "chainmail-helmet");
		items.put("303", "chainmail-chestplate");
		items.put("304", "chainmail-pants");
		items.put("305", "chainmail-boots");
		items.put("306", "iron-helmet");
		items.put("307", "iron-chestplate");
		items.put("308", "iron-pants");
		items.put("309", "iron-boots");
		items.put("310", "diamond-helmet");
		items.put("311", "diamond-chestplate");
		items.put("312", "diamond-pants");
		items.put("313", "diamond-boots");
		items.put("314", "gold-helmet");
		items.put("315", "gold-chestplate");
		items.put("316", "gold-pants");
		items.put("317", "gold-boots");
		items.put("318", "flint");
		items.put("319", "pork");
		items.put("320", "grilled-pork");
		items.put("321", "painting");
		items.put("322", "golden-apple");
		items.put("323", "sign");
		items.put("324", "wooden-door");
		items.put("325", "bucket");
		items.put("326", "water-bucket");
		items.put("327", "lava-bucket");
		items.put("328", "mine-cart");
		items.put("329", "saddle");
		items.put("330", "iron-door");
		items.put("331", "redstone");
		items.put("332", "snowball");
		items.put("333", "boat");
		items.put("334", "leather");
		items.put("335", "milk-bucket");
		items.put("336", "clay-brick");
		items.put("337", "clay-balls");
		items.put("338", "reed");
		items.put("339", "paper");
		items.put("340", "book");
		items.put("341", "slime-ball");
		items.put("342", "storage-mine-cart");
		items.put("343", "powered-mine-cart");
		items.put("344", "egg");
		items.put("345", "compass");
		items.put("346", "fishing-rod");
		items.put("347", "watch");
		items.put("348", "brittle-gold-dust");
		items.put("349", "raw-fish");
		items.put("350", "cooked-fish");
		items.put("2256", "gold-record");
		items.put("2257", "green-record");

		// Setup Timers
		this.mTime1 = new Timer();
		this.mTime2 = new Timer();

		// Start the ticking for giving
		if (this.moneyGiveInterval > 0) {
			this.mTime1.schedule(new TimerTask() {

				public void run() {
					etc.getInstance();
					List<Player> localList = etc.getServer().getPlayerList();
					for (Player localPlayer : localList) {
						iConomy.this.deposit(null, localPlayer.getName(), iConomy.this.moneyGive, false);
					}
				}
			}, 0L, this.moneyGiveInterval);
		}

		// Start the ticking for taking
		if (this.moneyTakeInterval > 0) {
			this.mTime2.schedule(new TimerTask() {

				public void run() {
					etc.getInstance();
					List<Player> localList = etc.getServer().getPlayerList();
					for (Player localPlayer : localList) {
						iConomy.this.debit(null, localPlayer.getName(), iConomy.this.moneyTake, false);
					}
				}
			}, 0L, this.moneyTakeInterval);
		}

		return true;
	}

	/* Versioning */
	private static String getVersion()
	{
		String content = null;

		// many of these calls can throw exceptions, so i've just
		// wrapped them all in one try/catch statement.
		try
		{
			URL url = new URL("http://mc.nexua.org/plugins/iConomy/?latest");
			URLConnection urlConnection = url.openConnection();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

			String line;

			// read from the urlconnection via the bufferedreader
			while ((line = bufferedReader.readLine()) != null){
				content = line;
			}
			bufferedReader.close();
		} catch(Exception e) {
			log.severe("[iConomy Version Check] " + e);
		}

		return content;
	}

	/* Misc Functions */
	String itemName(String id) {
		String name = (String) this.items.get(id);
		if (name != null) {
			return name;
		}

		return "";
	}

	public static String cInt(int i) {
		return Integer.toString(i);
	}

	public boolean hasItems(Player player, int item, int amount) {
		Inventory inv = player.getInventory();

		if (inv.hasItem(item, amount, 6400)) {
			return true;
		} else {
			return false;
		}
	}

	public void removeItems(Player player, int material, int amount) {
		Inventory inv = player.getInventory();
		inv.removeItem(new Item(material, amount));
		inv.updateInventory();
	}

	private boolean canAfford(String name, int cost) {
		return (cost <= this.data.getBalance(name)) ? true : false;
	}

	private Player getPlayer(String name) {
		etc.getInstance();
		return etc.getServer().getPlayer(name);
	}

	public void broadcast(String message)
	{
		for (Player p : etc.getServer().getPlayerList())
			p.sendMessage(message);
	}

	/* Logging */
	public static String logDate() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		return format.format(now);
	}

	public void shopLog(String type, String data) {
		String date = logDate();
		if (type.equalsIgnoreCase("pay") && this.logPay) {
			try {
				FileWriter fstream = new FileWriter(directory + lDirectory + "pay.log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(date + "|" + data);
				out.newLine();
				out.close();
			} catch (Exception es) {
				log.severe("[iConomy Pay Logging] " + es.getMessage());
			}
		} else if (type.equalsIgnoreCase("buy") && this.logBuy) {
			try {
				FileWriter fstream = new FileWriter(directory + lDirectory + "buy.log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(date + "|" + data);
				out.newLine();
				out.close();
			} catch (Exception es) {
				log.severe("[iConomy Buy Logging] " + es.getMessage());
			}
		} else if (type.equalsIgnoreCase("sell") && this.logSell) {
			try {
				FileWriter fstream = new FileWriter(directory + lDirectory + "sell.log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(date + "|" + data);
				out.newLine();
				out.close();
			} catch (Exception es) {
				log.severe("[iConomy Sell Logging] " + es.getMessage());
			}
		} else if (type.equalsIgnoreCase("auction") && this.logAuction) {
			try {
				FileWriter fstream = new FileWriter(directory + lDirectory + "auction.log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(date + "|" + data);
				out.newLine();
				out.close();
			} catch (Exception es) {
				log.severe("[iConomy Auction Loggin] " + es.getMessage());
			}
		}
	}

	/* Shop Item Functions */
	public int itemNeedsAmount(String type, String itemId) {
		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int cost = 0;

			try {
				conn = this.data.MySQL();
				if(type.equals("buy")) {
					ps = conn.prepareStatement("SELECT cost,perbundle FROM iBuy WHERE id = ? LIMIT 1");
				} else {
					ps = conn.prepareStatement("SELECT cost,perbundle FROM iSell WHERE id = ? LIMIT 1");
				}
				ps.setInt(1, Integer.parseInt(itemId));
				rs = ps.executeQuery();

				if (rs.next()) {
					if (rs.getInt("perbundle") > 1) {
						return rs.getInt("perbundle");
					} else {
						if (rs.getInt("cost") == 0) {
							return 0;
						} else {
							return 1;
						}
					}
				} else {
					return 0;
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the item [" + itemId + "] " + type + " price from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}

			return cost;
		} else {
			String info = "";

			if (type.equals("buy")) {
				info = this.buying.getString((String) this.items.get(itemId));
			} else if (type.equals("sell")) {
				info = this.selling.getString((String) this.items.get(itemId));
			}

			if (info.equals("")) {
				return 0;
			}

			if (info.contains(",")) {
				String[] item = info.split(",");
				return Integer.parseInt(item[0]);
			} else {
				return 1;
			}
		}
	}

	public boolean itemCan(String type, String itemId, int amount) {
		int itemAmount = this.itemNeedsAmount(type, itemId);

		// Maximum
		if (amount > 6400) {
			return false;
		}

		if (itemAmount == 0) {
			return false;
		} else if (itemAmount > 1) {
			if ((amount * itemAmount) < 6400) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public int itemCost(String type, String itemId, int amount, boolean total) {
		String info = "";
		int perBundle = 0;
		int cost = 0;

		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				if(type.equals("buy")) {
					ps = conn.prepareStatement("SELECT cost,perbundle FROM iBuy WHERE id = ? LIMIT 1");
				} else {
					ps = conn.prepareStatement("SELECT cost,perbundle FROM iSell WHERE id = ? LIMIT 1");
				}
				ps.setInt(1, Integer.parseInt(itemId));
				rs = ps.executeQuery();

				if (rs.next()) {
					if (!this.itemCan(type, itemId, amount)) {
						return 0;
					}

					// Settings
					cost = rs.getInt("cost");
					perBundle = rs.getInt("perbundle");

					if (total) {
						return cost*amount;
					} else {
						return cost;
					}
				} else {
					return 0;
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the item [" + itemId + "] " + type + " price from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}

			return cost;
		} else {
			if (type.equals("buy")) {
				info = this.buying.getString((String) this.items.get(itemId));
			} else if (type.equals("sell")) {
				info = this.selling.getString((String) this.items.get(itemId));
			}

			if (info.equals("")) {
				return 0;
			}

			if (info.contains(",")) {
				String[] item = info.split(",");
				perBundle = Integer.parseInt(item[0]);
				cost = Integer.parseInt(item[1]);

				// Check if we can
				if (!this.itemCan(type, itemId, amount)) {
					return 0;
				}

				if (total) {
					return cost*amount;
				} else {
					return cost;
				}
			} else {
				cost = Integer.parseInt(info);

				if (total) {
					return cost*amount;
				} else {
					return cost;
				}
			}
		}
	}

	/* Shop Listing Functions */
	public int listGetRows(String table) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int i = 0;

		try {
			conn = this.data.MySQL();

			if(table.equalsIgnoreCase("ibuy")){
				ps = conn.prepareStatement("SELECT * FROM iBuy");
			} else {
				ps = conn.prepareStatement("SELECT * FROM iSell");
			}

			rs = ps.executeQuery();

			while(rs.next()) { if(rs.getInt("cost") != 0) { i++; } }
		} catch (SQLException ex) {
			log.severe("[iConomy] Unable to count itemlist!");
		} finally {
			try {
				if (ps != null) { ps.close(); }
				if (rs != null) { rs.close(); }
				if (conn != null) { conn.close(); }
			} catch (SQLException ex) { }
		}

		return i;
	}

	public void showSellersList(Player player, int page) {
		List available = new ArrayList();
		Map aList;

		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int i = 1;

			int amount = page;

			if (amount > 0){
				amount = (amount - 1) * 10;
			} else {
				amount = 0;
			}

			int process = amount+10;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT id,cost,perbundle FROM iSell ORDER BY cost ASC");
				rs = ps.executeQuery();

				int rowCount = this.listGetRows("iSell");

				if(rowCount == 0) {
					player.sendMessage(Colors.Rose +"No items for selling."); return;
				}

				player.sendMessage(Colors.White + "Shop Selling (Page " + Colors.Green  + (page) + Colors.White + " of " + Colors.Green  + (int)Math.ceil(rowCount / 10.0D) + Colors.White + "):");

				i = amount;
				while (rs.next()) {
					if(i < process) {
						if(i > amount) {
							int itemId = rs.getInt("id");
							int cost = rs.getInt("cost");
							int perBundle = rs.getInt("perbundle");
							boolean bundle = false;

							if (cost == 0) {
								continue;
							}

							if(perBundle != 0) {
								bundle = true;
							}

							String name = this.itemName(cInt(itemId));

							if(bundle)
								player.sendMessage(Colors.Green + name + Colors.LightGray + " is worth " + Colors.Green + cost + this.moneyName + Colors.LightGray + " at " + Colors.Green + perBundle + Colors.LightGray + " per bundle.");
							else {
								player.sendMessage(Colors.Green + name + Colors.LightGray + " is worth " + Colors.Green + cost + this.moneyName + Colors.LightGray + " per item.");
							}
						}
						i++;
					} else {
						break;
					}
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the itemlist from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			try {
				aList = this.selling.returnMap();
			} catch (Exception ex) {
				log.info("[iConomy] Listing failed for selling list"); return;
			}

			for (Object key: aList.keySet()) {
				String cost = (String) aList.get(key);
				String name = (String) key;
				Boolean bundle = false;
				int perBundle = 0;

				if(cost.equals("0")) {
					continue;
				} else if(cost.contains(",")) {
					String[] item = cost.split(",");
					perBundle = Integer.parseInt(item[0]);
					cost = item[1];
					bundle = true;
				} else {
					bundle = false;
				}

				if(bundle)
					available.add(Colors.Green + name + Colors.LightGray + " is worth " + Colors.Green + cost + this.moneyName + Colors.LightGray + " at " + Colors.Green + perBundle + Colors.LightGray + " per bundle.");
				else {
					available.add(Colors.Green + name + Colors.LightGray + " is worth " + Colors.Green + cost + this.moneyName + Colors.LightGray + " per item.");
				}
			}

			if(available.isEmpty()) {
				player.sendMessage(Colors.Rose +"No items for selling."); return;
			}

			player.sendMessage(Colors.White + "Shop Selling (Page " + Colors.Green  + (page) + Colors.White + " of " + Colors.Green  + (int)Math.ceil(available.size() / 10.0D) + Colors.White + "):");

			try {
				int amount = page;

				if (amount > 0){
					amount = (amount - 1) * 10;
				} else {
					amount = 0;
				}

				for (int i = amount; i < amount + 10; i++)
					if (available.size() > i)
						player.sendMessage((String)available.get(i));
			}
			catch (NumberFormatException ex)
			{
				player.sendMessage("§cNot a valid page number.");
			}
		}
	}

	public void showBuyersList(Player player, int page) {
		List available = new ArrayList();
		Map aList;

		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int i = 1;

			int amount = page;

			if (amount > 0){
				amount = (amount - 1) * 10;
			} else {
				amount = 0;
			}

			int process = amount+10;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT id,cost,perbundle FROM iBuy ORDER BY cost ASC");
				rs = ps.executeQuery();

				int rowCount = this.listGetRows("iBuy");

				if(rowCount == 0) {
					player.sendMessage(Colors.Rose +"No items for buying."); return;
				}

				player.sendMessage(Colors.White + "Shop Buying (Page " + Colors.Green  + (page) + Colors.White + " of " + Colors.Green  + (int)Math.ceil(rowCount / 10.0D) + Colors.White + "):");

				while (rs.next()) {
					if(i < process) {
						if(i > amount) {
							int itemId = rs.getInt("id");
							int cost = rs.getInt("cost");
							int perBundle = rs.getInt("perbundle");
							boolean bundle = false;

							if (cost == 0) {
								continue;
							}

							if(perBundle != 0) {
								bundle = true;
							}

							String name = this.itemName(cInt(itemId));

							if(bundle)
								player.sendMessage(Colors.Green + name + Colors.LightGray + " costs " + Colors.Green + cost + this.moneyName + Colors.LightGray + " at " + Colors.Green + perBundle + Colors.LightGray + " per bundle.");
							else {
								player.sendMessage(Colors.Green + name + Colors.LightGray + " costs " + Colors.Green + cost + this.moneyName + Colors.LightGray + " per item.");
							}
						}
						i++;
					} else {
						break;
					}
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the itemlist from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			try {
				aList = this.buying.returnMap();
			} catch (Exception ex) {
				log.info("[iConomy] Listing failed for buying list"); return;
			}

			for (Object key: aList.keySet()) {
				String cost = (String) aList.get(key);
				String name = (String) key;
				Boolean bundle = false;
				int perBundle = 0;

				if(cost.equals("0")) {
					continue;
				} else if(cost.contains(",")) {
					String[] item = cost.split(",");
					perBundle = Integer.parseInt(item[0]);
					cost = item[1];
					bundle = true;
				} else {
					bundle = false;
				}

				if(bundle)
					available.add(Colors.Green + name + Colors.LightGray + " costs " + Colors.Green + cost + this.moneyName + Colors.LightGray + " at " + Colors.Green + perBundle + Colors.LightGray + " per bundle.");
				else {
					available.add(Colors.Green + name + Colors.LightGray + " costs " + Colors.Green + cost + this.moneyName + Colors.LightGray + " per item.");
				}
			}

			if(available.isEmpty()) {
				player.sendMessage(Colors.Rose +"No items for buying."); return;
			}

			player.sendMessage(Colors.White + "Shop Buying (Page " + Colors.Green  + (page) + Colors.White + " of " + Colors.Green  + (int)Math.ceil(available.size() / 10.0D) + Colors.White + "):");

			try {
				int amount = page;

				if (amount > 0){
					amount = (amount - 1) * 10;
				} else {
					amount = 0;
				}

				for (int i = amount; i < amount + 10; i++)
					if (available.size() > i)
						player.sendMessage((String)available.get(i));
			}
			catch (NumberFormatException ex)
			{
				player.sendMessage("§cNot a valid page number.");
			}
		}
	}

	/* Shop Functions */
	public boolean doPurchase(Player player, int itemId, int amount) {
		int itemAmount = this.itemCost("buy", cInt(itemId), amount, false);
		int needsAmount = this.itemNeedsAmount("buy", cInt(itemId));

		if(this.data.getBalance(player.getName()) < itemAmount){
			player.sendMessage(Colors.Rose + this.buyNotEnough);
			log.info("[iConomy Shop] " + "Player " + player.getName() + " attempted to buy more [" + itemId + "] [" + amount + "] than they have in "+this.moneyName+" [" + itemAmount + "].");
			this.shopLog("buy", player.getName() + "|0|203|" + itemId + "|" + amount + "|" + itemAmount + this.moneyName);

			if(debugging)
				log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] ["+amount+"] ["+itemAmount+"] [#20335]");
		}

		if (!this.itemCan("buy", cInt(itemId), amount)) {
			player.sendMessage(Colors.Rose + String.format(this.buyInvalidAmount, needsAmount));
			log.info("[iConomy Shop] " + "Player " + player.getName() + " attempted to buy bundle [" + itemId + "] with the offset amount [" + amount + "].");
			this.shopLog("buy", player.getName() + "|0|202|" + itemId + "|" + amount);

			if(debugging)
				log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] ["+needsAmount+"] ["+amount+"] [#20336]");

			return true;
		}

		if (itemAmount != 0) {
			int total = this.itemCost("buy", cInt(itemId), amount, true);
			String totalAmount = total + this.moneyName;

			if (this.data.getBalance(player.getName()) < total) {
				player.sendMessage(Colors.Rose + this.buyNotEnough);
				return true;
			}

			// Take dat money!
			this.debit(null, player.getName(), total, true);

			// Total giving
			int totalGive = (needsAmount > 1) ? needsAmount*amount : amount;

			// Give dat item!
			player.giveItem(itemId, totalGive);

			// Send Message
			player.sendMessage(Colors.Green + String.format(this.buyGive, totalAmount));
			log.info("[iConomy Shop] " + "Player " + player.getName() + " bought item [" + itemId + "] amount [" + amount + "] total [" + totalAmount + "].");
			this.shopLog("buy", player.getName() + "|1|200|" + itemId + "|" + amount + "|" + total);
		} else {
			// Send Message
			player.sendMessage(Colors.Rose + this.buyReject);
			log.info("[iConomy Shop] " + "Player " + player.getName() + " requested to buy an unavailable item: [" + itemId + "].");
			this.shopLog("buy", player.getName() + "|0|201|" + itemId);

			if(debugging)
				log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] [#20337]");

			return true;
		}

		return false;
	}

	public boolean doSell(Player player, int itemId, int amount) {
		Inventory bag = player.getInventory();
		int needsAmount = this.itemNeedsAmount("sell", cInt(itemId));

		if (!this.itemCan("sell", cInt(itemId), amount)) {
			player.sendMessage(Colors.Rose + String.format(this.sellInvalidAmount, needsAmount));
			log.info("[iConomy Shop] " + "Player " + player.getName() + " attempted to sell [" + amount + "] bundles of [" + itemId + "].");
			this.shopLog("sell", player.getName() + "|0|204|" + itemId + "|" + amount);

			if(debugging)
				log.info("[iConomy Debugging] [" + player.getName() + "] ["+amount+"] ["+needsAmount*amount+"] ["+itemId+"] [#20338]");

			return true;
		}

		int itemAmount = this.itemCost("sell", cInt(itemId), amount, false);

		if (itemAmount != 0) {
			int amt = (needsAmount > 1) ? needsAmount*amount : amount;
			int sold = 0;
			while (amt > 0) {
				if (bag.hasItem(itemId, ((amt > 64) ? 64 : amt), 6400)) {
					sold += ((amt > 64) ? 64 : amt);
					bag.removeItem(new Item(itemId, ((amt > 64) ? 64 : amt)));
					amt -= 64;
				} else {
					break;
				}
			}

			// Really didn't sell anything did we?
			if (sold == 0) {
				player.sendMessage(Colors.Rose + this.sellNone);
				log.info("[iConomy Shop] " + "Player " + player.getName() + " attempted to sell itemId [" + itemId + "] but had none.");
				this.shopLog("sell", player.getName() + "|0|202|" + itemId);

				if(debugging)
					log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] [#20339]");

				return true;
			} else {
				bag.updateInventory();
			}

			// Total
			int total = this.itemCost("sell", cInt(itemId), (sold/needsAmount), true);
			String totalAmount = total + this.moneyName;

			// Send Message
			player.sendMessage(Colors.LightGray + String.format(this.sellGiveAmount, sold, ((needsAmount > 1) ? needsAmount*amount : amount)));
			player.sendMessage(Colors.Green + String.format(this.sellGive, totalAmount));

			// Take dat money!
			this.deposit(null, player.getName(), total, false);

			// Show Balance
			showBalance(player.getName(), null, true);

			log.info("[iConomy Shop] " + "Player " + player.getName() + " sold item [" + itemId + "] amount [" + amount + "] total [" + totalAmount + "].");
			this.shopLog("sell", player.getName() + "|1|200|" + itemId + "|" + amount + "|" + total);
			return true;
		} else {
			// Send Message
			player.sendMessage(Colors.Rose + this.sellReject);
			log.info("[iConomy Shop] " + "Player " + player.getName() + " requested to sell an unsellable item: [" + itemId + "].");
			this.shopLog("sell", player.getName() + "|0|201|" + itemId);

				if(debugging)
					log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] [#20340]");

			return true;
		}
	}

	/* Money Functions */
	public void deposit(String pdata1, String pdata2, int amount, boolean really) {
		// Check
		Player player1 = this.getPlayer(pdata1);
		Player player2 = this.getPlayer(pdata2);

		// Balance
		int i = this.data.getBalance(pdata2);

		i += amount;
		this.data.setBalance(pdata2, i);

		if (really && player2 != null) {
			player2.sendMessage(Colors.Green + "You received " + amount + this.moneyName);
			showBalance(pdata2, null, true);

			if (player1 != null) {
				player1.sendMessage(Colors.Green + amount + this.moneyName + " deposited into " + pdata2 + "'s account");
			}

		}

		if(debugging)
			log.info("[iConomy Debugging] [" + player1 + "] [" + player2 + "] ["+amount+"] [" + really + "] [#20341]");

		if(player2 != null) {
			updateState(player2, true);
		}
	}

	public void debit(String pdata1, String pdata2, int amount, boolean really) {
		// Check
		Player player1 = this.getPlayer(pdata1);
		Player player2 = this.getPlayer(pdata2);

		// Balance
		int i = this.data.getBalance(pdata2);

		if (amount > i) {
			amount = i;
		}

		i -= amount;
		this.data.setBalance(pdata2, i);

		if (really && player2 != null) {
			player2.sendMessage(Colors.Green + amount + this.moneyName + " was deducted from your account.");
			showBalance(pdata2, null, true);

			if (player1 != null) {
				player1.sendMessage(Colors.Green + amount + this.moneyName + " removed from " + pdata2 + "'s account");
			}

		}

		if(debugging)
			log.info("[iConomy Debugging] [" + player1 + "] [" + player2 + "] ["+amount+"] [" + really + "] [#20342]");

		if(player2 != null) {
			updateState(player2, true);
		}
	}

	public void reset(String pdata, Player local, boolean notify) {
		// Check
		Player player = this.getPlayer(pdata);

		// Reset
		this.data.setBalance(pdata, this.startingBalance);

		// Notify
		if (notify) {
			if (player != null) {
				player.sendMessage(Colors.Green + "Your account has been reset.");
			}
		}

		// Notify the resetter and server regardless.
		local.sendMessage(Colors.Rose + pdata + "'s account has been reset.");
		log.info("[iConomy Money] " + pdata + "'s account has been reset by " + local.getName());

		if(debugging)
			log.info("[iConomy Debugging] [" + player + "] [" + local + "] [" + notify + "] [#20343]");

		// Update
		if (player != null) {
			updateState(player, true);
		}
	}

	public void pay(String pdata1, String pdata2, int amount) {
		// Check
		Player player1 = this.getPlayer(pdata1);
		Player player2 = this.getPlayer(pdata2);

		// Balances
		int i = this.data.getBalance(pdata1);
		int j = this.data.getBalance(pdata2);

		if (pdata1.equals(pdata2)) {
			if(player1 != null)
				player1.sendMessage(Colors.Rose + "You cannot send yourself money");

			if(debugging)
				log.info("[iConomy Debugging] [" + pdata1 + "] [" + pdata2 + "] [" + amount + "] [#20344]");
		} else if (amount > i) {
			if(player1 != null)
				player1.sendMessage(Colors.Rose + "You do not have enough money.");

			if(debugging)
				log.info("[iConomy Debugging] [" + pdata1 + "] [" + pdata2 + "] [" + amount + "] [#20345]");
		} else {
			// Update player one balance
			i -= amount;
			this.data.setBalance(pdata1, i);

			// Update player two balance
			j += amount;
			this.data.setBalance(pdata2, j);

			// Send messages
			if(player1 != null)
				player1.sendMessage(Colors.LightGray + "You have sent " + Colors.Green + amount + this.moneyName + Colors.LightGray + " to " + Colors.Green + pdata2);

			if(player2 != null)
				player2.sendMessage(Colors.Green + pdata1 + Colors.LightGray + " has sent you " + Colors.Green + amount + this.moneyName);

			// Log
			this.shopLog("pay", pdata1 + "|"+pdata2+"|1|200|" + amount);

			if(debugging)
				log.info("[iConomy Debugging] [" + pdata1 + "] [" + pdata2 + "] [" + amount + "] [#20346]");

			// Show each balance
			if(player1 != null)
				showBalance(pdata1, null, true);

			if(player2 != null)
				showBalance(pdata2, null, true);

			// Update each players state
			updateState(player1, false);
			updateState(player2, true);
		}
	}

	public int getBalance(Player player) {
		return this.data.getBalance(player.getName());
	}

	public void showBalance(String name, Player local, boolean isMe) {
		int i = this.data.getBalance(name);
		if (isMe) {
			Player player = this.getPlayer(name);
			player.sendMessage(Colors.LightGray + "Balance: " + Colors.Green + i + this.moneyName);
		} else {
			local.sendMessage(Colors.LightGray + name + "'s Balance: " + Colors.Green + i + this.moneyName);
		}
	}

	public void rank(String pdata1, String pdata2, boolean isMe) {
		Player player = this.getPlayer(pdata1);

		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int i = 1;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT player,balance FROM iBalances ORDER BY balance DESC");
				rs = ps.executeQuery();

				while (rs.next()) {
					if (isMe) {
						if (rs.getString("player").equalsIgnoreCase(pdata1)) {
							player.sendMessage(Colors.LightGray + "Your rank is " + Colors.Green + i); break;
						}
					} else {
						if (rs.getString("player").equalsIgnoreCase(pdata2)) {
							player.sendMessage(Colors.Green + rs.getString("player") + Colors.LightGray + " rank is " + Colors.Green + i); break;
						}
					}
					i++;
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the sqlrank for from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			Map accounts;
			TreeMap<String,Integer> sorted_accounts = null;
			ValueComparator bvc = null;
			int i = 1;

			try {
				accounts = this.data.accounts.returnMap();
				bvc =  new ValueComparator(accounts);
				sorted_accounts = new TreeMap(bvc);
				sorted_accounts.putAll(accounts);
			} catch (Exception ex) {
				Logger.getLogger(iConomy.class.getName()).log(Level.SEVERE, null, ex);
			}

			for (Object key : sorted_accounts.keySet()) {
				String name = (String) key;

				if (isMe) {
					if (name.equalsIgnoreCase(pdata1)) {
						player.sendMessage(Colors.LightGray + "Your rank is " + Colors.Green + i); break;
					}
				} else {
					if (name.equalsIgnoreCase(pdata2)) {
						player.sendMessage(Colors.Green + name + Colors.LightGray + " rank is " + Colors.Green + i); break;
					}
				}

				i++;
			}
		}
	}

	public void top(Player player, int amount) {
		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int i = 1;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT player,balance FROM iBalances ORDER BY balance DESC LIMIT 0,?");
				ps.setInt(1, amount);
				rs = ps.executeQuery();
				player.sendMessage(Colors.LightGray + "Top " + Colors.Green + amount + Colors.LightGray + " Richest People:");

				while (rs.next()) {
					player.sendMessage(Colors.LightGray + "   " + i + ". " + Colors.Green + rs.getString("player") + Colors.LightGray + " - " + Colors.Green + rs.getInt("balance") + this.moneyName);
					i++;
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the sqltop from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}

		} else {
			Map accounts;
			TreeMap<String,Integer> sorted_accounts = null;
			ValueComparator bvc = null;
			int i = 1;

			try {
				accounts = this.data.accounts.returnMap();
				bvc =  new ValueComparator(accounts);
				sorted_accounts = new TreeMap(bvc);
				sorted_accounts.putAll(accounts);
			} catch (Exception ex) {
				log.severe("[iConomy] Unable to retrieve array of balances!");
			}

			player.sendMessage(Colors.LightGray + "Top " + Colors.Green + amount + Colors.LightGray + " Richest People:");

			if (sorted_accounts.size() < 1) {
				player.sendMessage(Colors.LightGray + "   Nobody Yet!"); return;
			}

			if (amount > sorted_accounts.size()) {
				amount = sorted_accounts.size();
			}

			for (Object key : sorted_accounts.keySet()) {
				String name = (String) key;
				String balance = "" + sorted_accounts.get(name);

				if(i <= amount) {
					// Send top players
					player.sendMessage(Colors.LightGray + "   " + i + ". " + Colors.Green + name + Colors.LightGray + " - " + Colors.Green + balance + this.moneyName);
				} else {
					break;
				}

				i++;
			}
		}
	}

	/* Auction Functions */
	public boolean startAuction(Player player, int inter, int itemId, int itemAmount, int startingBid, int minBid, int maxBid) {
		Inventory bag = player.getInventory();
		int amt = itemAmount;
		int sold = 0;
		while (amt > 0) {
			if (bag.hasItem(itemId, ((amt > 64) ? 64 : amt), 6400)) {
				sold = sold + ((amt > 64) ? 64 : amt);
				bag.removeItem(new Item(itemId, (amt > 64 ? 64 : amt)));
				amt -= 64;
			} else {
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Yellow + "Auctioner has attempted to cheat!");
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Yellow + "Specified items are not in his inventory!");

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] [" + inter + "] [" + itemId + "] [" + startingBid + "] [" + minBid + "] [" + maxBid + "] [#20348]");

				return false;
			}
		}

		// Really didn't sell anything did we?
		if (sold == 0) {
			this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Yellow + "Auctioner has attempted to cheat!");
			this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Yellow + "Specified items are not in his inventory!");

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] [" + inter + "] [" + itemId + "] [" + startingBid + "] [" + minBid + "] [" + maxBid + "] [#20349]");


			this.shopLog("auction", player.getName() + "|null|0|205|"+itemId+"|"+inter+"|"+startingBid+"|"+minBid+"|"+maxBid);

			return false;
		} else {
			bag.updateInventory();
		}

		this.auctionTimerRunning = true;
		this.auctionItem = itemId;
		this.auctionAmount = itemAmount;
		this.auctionStarter = player.getName();
		this.auctionCurBid = 0;
		this.auctionCurSecretBid = 0;
		this.auctionCurAmount = startingBid;
		this.auctionStartingBid = startingBid;
		this.auctionCurBidCount = 0;
		this.auctionMin = minBid;
		this.auctionReserve = maxBid;

		this.shopLog("auction", player.getName() + "|null|1|205|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);

		// Setup finals
		final iConomy iHateJava = this;
		final int interval = inter;

		// The timer whoops.
		this.auctionTimer = new Timer();

		// Start
		auctionTimer.scheduleAtFixedRate(new TimerTask() {
		    int i = interval;
		    iConomy p = iHateJava;
		    public void run() {
			this.i--;
			if (i == 10) {
				this.p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + "10" + Colors.LightGray + " seconds left to bid!");
			}

			if (i < 6 && i > 1) {
				this.p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + i + Colors.LightGray + " seconds left to bid!");
			}

			if(i == 1) {
				this.p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + i + Colors.LightGray + " second left to bid!");
			}

			if (i < 1) { this.p.endAuction(); }
		    }
		}, 0L, 1000);

		return true;
	}

	public void bidAuction(Player player, int amount, int secret) {
		Boolean outbid = true;
		if(this.auctionCurAmount < amount) {
			if(this.auctionCurBid != 0 && this.auctionCurAmount != this.auctionStartingBid) {
				if(amount < this.auctionCurSecretBid) {
					if(secret != 0 && secret > this.auctionCurSecretBid) {
						if(secret == this.auctionCurSecretBid) {
							amount = secret;
							secret = 0;
						} else {
							amount = this.auctionCurSecretBid+1;
						}
					} else {
						outbid = false;
					}

					if(!outbid) {
						this.auctionCurBid = amount+1;
						this.auctionCurAmount = amount+1;
						this.auctionCurBidCount += 1;

						if(this.auctionCurBid == this.auctionCurSecretBid) {
							Player previous = this.getPlayer(this.auctionCurName);

							if(previous != null) {
								previous.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "has reached your secret amount!");
							}
						}

						this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] has been raised to " + Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + "!");
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "You have been outbid by "+ Colors.Green + this.auctionCurName + Colors.LightGray + " secret bid!");

						if(debugging)
							log.info("[iConomy Debugging] [" + player + "] [" + amount + "] [" + secret + "] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20350]");

						this.shopLog("auction", player.getName() + "|"+this.auctionCurName+"|1|200|" + amount + "|"+secret+"|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);

					} else {
						Player previous = this.getPlayer(this.auctionCurName);

						if(previous != null) {
							previous.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "You have been outbid!");
						}
					}
				} else {
					Player previous = this.getPlayer(this.auctionCurName);

					if(previous != null) {
						previous.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "You have been outbid!");
					}
				}
			}

			if(outbid) {
				this.auctionCurBid = amount;
				this.auctionCurSecretBid = (secret == 0) ? amount : secret;
				this.auctionCurAmount = amount;
				this.auctionCurName = player.getName();
				this.auctionCurBidCount += 1;

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] [" + amount + "] [" + secret + "] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20351]");

				this.shopLog("auction", player.getName() + "|"+this.auctionCurName+"|1|201|" + amount + "|"+secret+"|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
			}

			if(this.auctionCurAmount >= this.auctionReserve && this.auctionReserve != 0 && !this.auctionReserveMet) {
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + this.auctionCurName + Colors.LightGray + " reserve has been met!");
				this.auctionReserveMet = true;

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] [" + amount + "] [" + secret + "] ["+this.auctionCurAmount+"] ["+this.auctionReserve+"] ["+this.auctionReserveMet+"] [#20352]");

				this.shopLog("auction", player.getName() + "|"+this.auctionCurName+"|1|202|" + amount + "|"+secret+"|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
			} else {
				if(outbid) {
					this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + this.auctionCurName + Colors.LightGray + " is now in the lead!");
					this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.LightGray + "Auction currently stands at " + Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + "!");
				}
			}
		} else {
			player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "You must bid over the auction amount!");

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] [" + amount + "] [" + secret + "] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20351]");

			this.shopLog("auction", player.getName() + "|"+this.auctionCurName+"|0|203|" + amount + "|"+secret+"|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
		}
	}

	public void endAuction() {
		auctionTimer.cancel();
		this.auctionTimerRunning = false;

		if(this.auctionCurBid != 0 && this.auctionCurAmount != this.auctionStartingBid) {
			if(this.auctionCurAmount > this.auctionMin) {
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + this.auctionCurName + Colors.LightGray + " has won the auction at " + Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + "!");

				Player player = this.getPlayer(this.auctionCurName);
				Player auctioner = this.getPlayer(this.auctionStarter);

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] ["+auctioner+"] ["+this.auctionItem+"] ["+this.auctionAmount+"] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20352]");

				this.shopLog("auction", player.getName() + "|"+this.auctionStarter+"|1|204|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);

				if(player == null && auctioner == null) {
					this.auctions.setString(this.auctionCurName, this.auctionItem + "," + this.auctionAmount + "," + this.auctionCurAmount);
					this.auctioner.setInt(this.auctionStarter, this.auctionCurAmount);
				} else if (player == null && auctioner != null) {
					this.auctions.setString(this.auctionCurName, this.auctionItem + "," + this.auctionAmount + "," + this.auctionCurAmount);

					// Weee~
					this.deposit(null, auctioner.getName(), this.auctionCurAmount, false);
					auctioner.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "Auction Over!");
					auctioner.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + " has been credited to your account!");
				} else if (player != null && auctioner == null) {
					this.debit(null, player.getName(), this.auctionCurAmount, false);
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "You Won! ");
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + " has been debited from your account!");
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Enjoy your item(s)!");
					player.giveItem(this.auctionItem, this.auctionAmount);

					this.auctioner.setInt(this.auctionStarter, this.auctionCurAmount);
				} else {
					this.debit(null, player.getName(), this.auctionCurAmount, false);
					this.deposit(null, auctioner.getName(), this.auctionCurAmount, false);

					auctioner.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "Auction Over!");
					auctioner.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + " has been credited to your account!");

					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "You Won! ");
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + " has been debited from your account!");
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Enjoy your item(s)!");

					// Give items! :D
					player.giveItem(this.auctionItem, this.auctionAmount);
				}
			} else {
				Player player = this.getPlayer(this.auctionStarter);

				if(player != null) {
					player.giveItem(this.auctionItem, this.auctionAmount);
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Item(s) have been returned to you!");
				}

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] ["+this.auctionCurName+"] ["+this.auctionItem+"] ["+this.auctionAmount+"] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20353]");

				this.shopLog("auction", player.getName() + "|"+this.auctionStarter+"|0|204|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + " Has ended. No winner as the minimum bid was not met.");
			}
		} else {
			Player player = this.getPlayer(this.auctionStarter);

			if(player != null) {
				player.giveItem(this.auctionItem, this.auctionAmount);
				player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Item(s) have been returned to you!");
			}


			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] ["+this.auctionCurName+"] ["+this.auctionItem+"] ["+this.auctionAmount+"] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20354]");


			this.shopLog("auction", player.getName() + "|"+this.auctionStarter+"|-1|204|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
			this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + " Has ended with no bids.");
		}

		this.auctionItem = 0;
		this.auctionAmount = 0;
		this.auctionStarter = "";
		this.auctionCurBid = 0;
		this.auctionCurSecretBid = 0;
		this.auctionCurAmount = 0;
		this.auctionStartingBid = 0;
		this.auctionCurBidCount = 0;
		this.auctionMin = 0;
		this.auctionReserve = 0;
	}

	public boolean wonAuction(String name) {
		return this.auctions.keyExists(name);
	}

	public boolean realAuction(String name) {
		return this.auctions.getString(name).contains(",");
	}

	public String[] parseAuction(String name) {
		String pauction = this.auctions.getString(name);
		return pauction.split(",");
	}

	public boolean hasAuctions(String name) {
		return this.auctioner.keyExists(name);
	}

	public boolean auctionFailed(String name) {
		return this.auctioner.getString(name).contains(",");
	}

	public String[] parseAuctioner(String name) {
		String auction = this.auctioner.getString(name);
		return auction.split(",");
	}

	public int auctionTotal(String name) {
		return this.auctioner.getInt(name);
	}

	public void auctionItems(Player player) {
		String name = player.getName();
		if(!realAuction(name)){
			return;
		}
		String[] pauction = parseAuction(name);
		int itemId = Integer.parseInt(pauction[0]);
		int itemAmount = Integer.parseInt(pauction[1]);
		int cost = Integer.parseInt(pauction[2]);
		this.debit(null, name, cost, false);
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "You Won the auction!");
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + cost + this.moneyName + Colors.LightGray + " has been debited from your account!");
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Enjoy your item(s)!");
		player.giveItem(itemId, itemAmount);

		if(debugging)
			log.info("[iConomy Debugging] [" + player + "] ["+itemId+"] ["+itemAmount+"] ["+cost+"] [#20355]");

		this.auctions.removeKey(name);
	}

	public void auctionerItems(Player player) {
		String name = player.getName();
		if(auctionFailed(name)){
			String[] pauction = parseAuctioner(name);
			player.giveItem(Integer.parseInt(pauction[0]), Integer.parseInt(pauction[1]));
			player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Item(s) have been returned to you!");
			player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + " Had ended with no bids or your min/max was not met.");
			this.auctioner.removeKey(name);

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] ["+pauction[0]+"] ["+pauction[1]+"] [#20356]");

			return;
		}
		int total = auctionTotal(name);
		this.deposit(null, name, total, false);
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "Auction Ended!");
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + total + this.moneyName + Colors.LightGray + " has been credited to your account!");

		if(debugging)
			log.info("[iConomy Debugging] [" + player + "] ["+total+"] [#20357]");

		this.auctioner.removeKey(name);
	}

	/* Lottery Functions */
	public String lotteryPrize() {
		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT * FROM `iPrizes` ORDER BY RAND() LIMIT 1");
				rs = ps.executeQuery();

				if (rs.next()) {
					// Settings
					int percent = rs.getInt("percent");
					int item = rs.getInt("item");
					int amount = rs.getInt("amount");

					return ""+percent+";"+item+";"+amount+";";
				} else {
					return "";
				}
			} catch (SQLException ex) {
				log.severe("[iConomy Lottery] Unable to grab a prize!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}

			return "";
		} else {
			Random generator = new Random();
			Object[] values = this.hashPrizes.values().toArray();

			if(values.length < 1) {
				return "";
			}

			String randomValue = (String) values[generator.nextInt(values.length)];
			return randomValue;
		}
	}

	public void lottery(Player player) {
		if(this.data.getBalance(player.getName()) < this.ticketCost) {
			player.sendMessage(this.lotteryTag + Colors.Rose + " You do not have enough to purchase a ticket!");
			player.sendMessage(this.lotteryTag + Colors.Rose + " Ticket cost: "+ this.ticketCost);

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] ["+this.data.getBalance(player.getName())+"] [#20358]");

			 return;
		} else {
			String prize = this.lotteryPrize();
			Random generator = new Random();

			// Do lottery Ticket
			this.debit(null, player.getName(), this.ticketCost, false);
			player.sendMessage(this.lotteryTag + Colors.LightGray + " " + String.format(this.lotteryCost, this.ticketCost + this.moneyName));

			// Do lottery checks
			if(prize.equals("")) {
				player.sendMessage(this.lotteryTag + Colors.Rose + " " + this.lotteryNotAvailable); return;
			}

			String[] data = prize.split(";");
			int percent = Integer.parseInt(data[0]);
			int item = Integer.parseInt(data[1]);
			int amount = Integer.parseInt(data[2]);
			int chance = generator.nextInt(100);
			int amountGiven = (amount > 1) ? generator.nextInt(amount) : 1;
			String itemName = this.itemName(cInt(item));
			itemName.replace("-", " ");

			if(chance < percent) {
				if(amountGiven != 0) {
					player.giveItem(item, amountGiven);
					this.broadcast(this.lotteryTag + Colors.Gold + " " + String.format(this.lotteryWinner, player.getName(), amountGiven, itemName));
					// player.sendMessage(this.lotteryTag + Colors.Gold + " Enjoy your items :)!");
				} else {
					player.sendMessage(this.lotteryTag + Colors.LightGray + " " + this.lotteryLoser);
					// this.broadcast(this.lotteryTag + Colors.LightGray + " " + this.lotteryLoser);
				}
			} else {
				player.sendMessage(this.lotteryTag + Colors.LightGray + " " + this.lotteryLoser);
			}

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] ["+this.data.getBalance(player.getName())+"] ["+amount+"] ["+amountGiven+"] ["+item+"] ["+percent+"] ["+chance+"] ["+itemName+"] [#20359]");

		}
	}

	/* Sign Shop */
	private void updateClick(String name) {
		lastClick.put(name, System.currentTimeMillis() / 1000L);
	}

	private boolean waitedEnough(String name) {
		if (lastClick.containsKey(name)) {
			long previous = lastClick.get(name);
			if ((System.currentTimeMillis() / 1000L) - previous > this.signWaitAmount) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	private void upgradeOwn(String name) {
		Player player = this.getPlayer(name);

		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT usable FROM iSignOwners WHERE owner=? LIMIT 1");
				ps.setString(1, name);
				rs = ps.executeQuery();

				if(rs.next()) {
					int currentAmount = rs.getInt("usable");
					int nextAmount = (currentAmount/this.signOwnAmount)*this.signOwnUpgrade;

					if(this.data.getBalance(name) < nextAmount){
						if(player != null)
							player.sendMessage(String.format(this.signUpgradeAmount, nextAmount + this.moneyName));

					} else {
						this.debit(null, name, nextAmount, true);
						conn = this.data.MySQL();

						ps = conn.prepareStatement("UPDATE iSignOwners SET usable=usable+? WHERE owner=? LIMIT 1");
						ps.setInt(1, this.signOwnAmount);
						rs = ps.executeQuery();
					}
				} else {
					if(player != null)
						player.sendMessage(this.signUpgradeExists);
				}
			} catch (Exception ex) {
				log.log(Level.SEVERE, "[iConomy] Upgrading failed!", ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			int currentAmount = this.signOwners.getInt(name);

			if(currentAmount == 0) {
				if(player != null)
					player.sendMessage(this.signUpgradeExists);
			}

			int nextAmount = (currentAmount/this.signOwnAmount)*this.signOwnUpgrade;

			if(this.data.getBalance(name) < nextAmount){
				if(player != null)
					player.sendMessage(String.format(this.signUpgradeAmount, nextAmount + this.moneyName));

			} else {
				this.debit(null, name, nextAmount, true);
				this.signOwners.setInt(name, currentAmount+this.signOwnAmount);
			}
		}
	}

	private int canOwn(String name) {
		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT usable FROM iSignOwners WHERE owner=? LIMIT 1");
				ps.setString(1, name);
				rs = ps.executeQuery();

				return (rs.next()) ? rs.getInt("usable") : this.signOwnAmount;
			} catch (Exception ex) {
				log.log(Level.SEVERE, "[iConomy] Listing failed for buying list", ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			return (this.signOwners.getInt(name) != 0) ? this.signOwners.getInt(name) : this.signOwnAmount;
		}

		return 0;
	}

	private void signStock(Player player) {
		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT item,amount FROM iSign WHERE owner=? ORDER BY item ASC");
				ps.setString(1, player.getName());
				rs = ps.executeQuery();

				player.sendMessage(Colors.White + "Shop stock ["+ Colors.Green + "item"+ Colors.White + " - "+ Colors.Green + "amount"+ Colors.White + "]");

				while (rs.next()) {
					player.sendMessage(this.itemName(rs.getString("item")) + " - " + rs.getString("amount"));
				}

			} catch (Exception ex) {
				log.log(Level.SEVERE, "[iConomy] Listing failed for buying list", ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			Map stockList = null;

			try {
				stockList = this.sign.returnMap();
			} catch (Exception ex) {
				log.info("[iConomy] Failed to create stock list" + ex);
			}

			for (Object key: stockList.keySet()) {
				String shop = (String) key;
				int stock = Integer.parseInt((String) stockList.get(key));
				String[] data = shop.split("-");
				String name = data[0];
				String item = data[1];

				if(name.equalsIgnoreCase(player.getName())){
					player.sendMessage(this.itemName(item) + " - " + stock);
				}
			}
		}
	}

	private int signCurStock(String name, int i) {
		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT amount FROM iSign WHERE owner=? AND item=? LIMIT 1");
				ps.setString(1, name);
				ps.setInt(2, i);
				rs = ps.executeQuery();


				if(rs.next()) {
					return rs.getInt("amount");
				} else {
					return 0;
				}

			} catch (Exception ex) {
				log.log(Level.SEVERE, "[iConomy] Listing failed for buying list", ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			return this.sign.getInt(name+"-"+i);
		}

		return 0;
	}

	public int ownSign(String name) {
		int i = 0;

		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT owner FROM iSign WHERE owner=?");
				ps.setString(1, name);
				rs = ps.executeQuery();

				while(rs.next()) {
					if(rs.getString("owner").equalsIgnoreCase(name)) {
						i++;
					}
				}
			} catch (SQLException ex) {
				log.info("[iConomy] Failed to grab sign data: " + ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			Map signShopsList = null;

			try {
				signShopsList = this.sign.returnMap();
			} catch (Exception ex) {
				log.info("[iConomy] Failed to create list");
			}

			for (Object key: signShopsList.keySet()) {
				String shop = (String) key;
				String[] data = shop.split("-");
				String owner = data[0];

				if(owner.equalsIgnoreCase(name)){
					i++;
				}
			}
		}

		return i;
	}

	public void deleteSign(String name, int i) {
		if(!this.existsSign(name,i))
			return;

		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("DELETE FROM iSign WHERE owner = ? AND item=? LIMIT 1");
				ps.setString(1, name); ps.setInt(2, i); ps.executeUpdate();

				ps = conn.prepareStatement("DELETE FROM iSignLocation WHERE owner = ? AND item=?");
				ps.setString(1, name); ps.setInt(2, i); ps.executeUpdate();
			} catch (SQLException ex) {
				log.info("[iConomy] Failed to delete Sign Shop: " + ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			this.sign.removeKey(name+"-"+i);
			this.signLocation.removeKey(name+"-"+i);
		}
	}

	public void updateSign(String name, int i, int s) {
		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("UPDATE iSign SET amount=? WHERE owner=? AND item=? LIMIT 1");
				ps.setInt(1, s);
				ps.setString(2, name);
				ps.setInt(3, i);
				ps.executeUpdate();
			} catch (SQLException ex) {
				log.info("[iConomy] Failed to update Sign Shop: " + ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {

			this.sign.setInt(name+"-"+i, s);
		}
	}

	private Boolean existsSign(String name, int i) {
		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT id FROM iSign WHERE owner=? AND item=? LIMIT 1");
				ps.setString(1, name);
				ps.setInt(2, i);
				rs = ps.executeQuery();

				return (rs.next()) ? true : false;
			} catch (SQLException ex) {
				log.info("[iConomy] Failed to check Sign Shop: " + ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			return (this.sign.getString(name+"-"+i).isEmpty()) ? false : true;
		}

		return false;
	}

	private void setSign(String name, int x, int y, int z, int i, int s) {
		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();

				if(!this.existsSign(name, i)) {
					ps = conn.prepareStatement("INSERT IGNORE INTO iSign (owner,item,amount) VALUES(?,?,?)");
					ps.setString(1, name);
					ps.setInt(2, i);
					ps.setInt(3, s);
					ps.executeUpdate();
				}

				if(this.signOwnUse)
					ps = conn.prepareStatement("INSERT IGNORE INTO iSignOwners(owner,usable) VALUES(?,?)");
					ps.setString(1, name);
					ps.setInt(2, this.signOwnAmount);
					ps.executeUpdate();

				ps = conn.prepareStatement("INSERT INTO iSignLocation (owner,item,x,y,z) VALUES(?,?,?,?,?)");
				ps.setString(1, name);
				ps.setInt(2, i);
				ps.setInt(3, x);
				ps.setInt(4, y);
				ps.setInt(5, z);
				ps.executeUpdate();
			} catch (SQLException ex) {
				log.info("[iConomy] Failed to create Sign Shop: " + ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			if(this.sign.getInt(name+"-"+i) == 0 || this.sign.getInt(name+"-"+i) == s)
				this.sign.setInt(name+"-"+i, s);

			if(this.signOwners.getString(name) == null ? "" == null : this.signOwners.getString(name).equals("") && this.signOwnUse)
				this.signOwners.setInt(name, this.signOwnAmount);

			this.signLocation.setString(name+"-"+i, this.signLocation.getString(name+"-"+i) + x +";"+y+";"+z+";|");
		}
	}

	public void signPush(Player player, String owner, int i, int amount, boolean isOwner, int price, Block blockClicked) {
		if (!hasItems(player, i, amount)) {
			player.sendMessage("You dont have enough" + this.itemName(cInt(i)));
			return;
		}

		int stock = signCurStock(owner, i);

		if (amount + stock > this.signMaxAmount) {
			if (isOwner) {
				player.sendMessage("Sorry dude, your stock is full.");
			} else {
				player.sendMessage("Sorry, " + owner + "s stock is currently full.");
			}
			return;
		} else {
			if(this.existsSign(owner, i)) {
				this.updateSign(owner, i, amount+stock);
			} else {
				this.setSign(owner, blockClicked.getX(), blockClicked.getY(), blockClicked.getZ(), i, amount+stock);
			}

			this.updateSigns(owner, i, (amount + stock));

			if (isOwner) {
				removeItems(player, i, amount);
				player.sendMessage(String.format(this.signStocked, amount, this.itemName(cInt(i))));
			} else {
				removeItems(player, i, amount);
				this.debit(null, owner, price, true);
				this.deposit(null, player.getName(), price, true);
				player.sendMessage(String.format(this.signSold, amount, this.itemName(cInt(i)), price+this.moneyName));
			}
		}

	}

	public void signPull(Player player, String owner, int i, int amount, boolean isOwner, int price) {
		int stock = signCurStock(owner, i);
		if(stock != 0) {
			if (amount <= stock) {
				this.updateSign(owner, i, (stock-amount));

				if (isOwner) {
					player.sendMessage(String.format(this.signLeftAmount, (stock-amount), this.itemName(cInt(i))));
				}

				player.giveItem(i, amount);

				this.updateSigns(owner, i, (stock-amount));

				if (!isOwner) {
					this.deposit(null, owner, price, true);
					this.debit(null, player.getName(), price, true);

					player.sendMessage(String.format(this.signBoughtAmount, amount, this.itemName(cInt(i)), price+this.moneyName));
				}

			} else {
				player.sendMessage(String.format(this.signNotEnough, owner, this.itemName(cInt(i))));
			}

		} else {
			player.sendMessage(owner + " does not have that item in stock");
		}
	}

	public void updateSigns(String name, int i, int stock) {
		if(this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT * FROM iSignLocation WHERE owner=? AND item=?");
				ps.setString(1, name);
				ps.setInt(2, i);
				rs = ps.executeQuery();

				while(rs.next()) {
					if(debugging)
						log.info("[iConomy Debugging] "+ rs.getInt("x") + " - "+ rs.getInt("y") + " - "+ rs.getInt("z"));

					int x = rs.getInt("x");
					int y = rs.getInt("y");
					int z = rs.getInt("z");

					if(this.updateSignLocation(x, y, z, 0, 1, 0, i, stock))
						continue;

					if(this.updateSignLocation(x, y, z, 0, -1, 0, i, stock))
						continue;

					if(this.updateSignLocation(x, y, z, 1, 0, 0, i, stock))
						continue;

					if(this.updateSignLocation(x, y, z, -1, 0, 0, i, stock))
						continue;

					if(this.updateSignLocation(x, y, z, 0, 0, 1, i, stock))
						continue;

					if(this.updateSignLocation(x, y, z, 0, 0, -1, i, stock))
						continue;
				}
			} catch (SQLException ex) {
				log.info("[iConomy] Failed to grab sign data: " + ex);
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			Map signShopsList = null;

			try {
				signShopsList = this.sign.returnMap();
			} catch (Exception ex) {
				log.info("[iConomy] Failed to create list");
			}

			for (Object key: signShopsList.keySet()) {
				String shop = (String) key;
				String[] data = shop.split("-");
				String owner = data[0];
				int item = Integer.parseInt(data[1]);

				if(owner.equalsIgnoreCase(name) && item == i){
					String sLocations = this.signLocation.getString(owner+"-"+item);
					String[] shopData = sLocations.split("\\|");

					if(debugging)
						log.info("[iConomy Debugging] "+Arrays.toString(shopData));

					for (String row : shopData) {
						if(row.contains(";") && !row.equals(";")) {
							String[] shopCData = row.split(";");

							if(debugging)
								log.info("[iConomy Debugging] " + row);

							if(shopCData[0] == null ? "" == null : shopCData[0].equals("") || shopCData[0].equals("|"))
								continue;

							int x = Integer.parseInt(shopCData[0]);
							int y = Integer.parseInt(shopCData[1]);
							int z = Integer.parseInt(shopCData[2]);

							if(debugging)
								log.info(x + " - " + y + " - " + z);

							if(this.updateSignLocation(x, y, z, 0, 1, 0, i, stock))
								continue;

							if(this.updateSignLocation(x, y, z, 0, -1, 0, i, stock))
								continue;

							if(this.updateSignLocation(x, y, z, 1, 0, 0, i, stock))
								continue;

							if(this.updateSignLocation(x, y, z, -1, 0, 0, i, stock))
								continue;

							if(this.updateSignLocation(x, y, z, 0, 0, 1, i, stock))
								continue;

							if(this.updateSignLocation(x, y, z, 0, 0, -1, i, stock))
								continue;
						}
					}
				}
			}
		}
	}

	public boolean updateSignLocation(int x, int y, int z, int a, int b, int c, int i, int stock){
		Sign theSign;
		ComplexBlock theblock = etc.getServer().getComplexBlock((x+a), (y+b), (z+c));

		if (!(theblock instanceof Sign)) {
			if(debugging)
				log.info("No sign");

			return false;
		} else {
			theSign = (Sign) theblock;

			if ((theSign.getText(1).equalsIgnoreCase("sell") || theSign.getText(1).equalsIgnoreCase("buy"))) {
				if(debugging)
					log.info("sign equaled sell or buy");

				return false;
			}

			theSign.setText(1, this.itemName(cInt(i)));
			theSign.setText(3, "In stock: " + stock);
			theSign.update();

			if(debugging)
				log.info("I updated at "+ (x+a) + "-" + (y+b) + "-" + (z+c));

			return true;
		}
	}

	/* Permissions */
	public boolean canDo(String can, Player player) {
	    if (!can.equals("*")) {
		    String[] groupies = can.split(",");
		    for (String group : groupies){ if(player.isInGroup(group)){return true;}}
		    return false;
	    }
	    return true;
	}

	public boolean can(Player player, String command) {
		if (command.equals("pay")) {
			return this.canDo(this.canPay, player);
		} else if (command.equals("debit")) {
			return this.canDo(this.canDebit, player);
		} else if (command.equals("credit")) {
			return this.canDo(this.canCredit, player);
		} else if (command.equals("reset")) {
			return this.canDo(this.canReset, player);
		} else if (command.equals("rank")) {
			return this.canDo(this.canRank, player);
		} else if (command.equals("view")) {
			return this.canDo(this.canView, player);
		} else if (command.equals("top")) {
			return this.canDo(this.canTop, player);
		} else if (command.equals("sell")) {
			return this.canDo(this.canSell, player);
		} else if (command.equals("buy")) {
			return this.canDo(this.canBuy, player);
		} else if (command.equals("bid")) {
			return this.canDo(this.canBid, player);
		} else if (command.equals("auction")) {
			return this.canDo(this.canAuction, player);
		} else if (command.equals("end")) {
			return this.canDo(this.canEnd, player);
		} else if (command.equals("lottery")) {
			return this.canDo(this.canLottery, player);
		} else if (command.equals("sign")) {
			return this.canDo(this.canSign, player);
		} else if (command.equals("signBuy")) {
			return this.canDo(this.canSignBuy, player);
		} else if (command.equals("signSell")) {
			return this.canDo(this.canSignSell, player);
		} else if (command.equals("trade")) {
			return this.canDo(this.canTrade, player);
		}

		return false;
	}

	public class Listener extends PluginListener {

		iConomy p;

		Listener(iConomy plugin) {
			p = plugin;
		}

		public void onLogin(Player player) {
			updateState(player, true);

			if(p.wonAuction(player.getName())) {
				p.auctionItems(player);
			}

			if(p.hasAuctions(player.getName())){
				p.auctionerItems(player);
			}
		}

		public boolean onCommand(Player player, String[] split) {
			if (!player.canUseCommand(split[0])) {

				if(debugging)
					log.info("[iConomy Debugging] ["+split[0]+"] [cannot use command] [#20360]");

				return false;
			}

			// Player stuff
			Player localPlayer;
			int i;

			/*
			 *  iConomy [Money]
			 *
			 *  @author: Nijikokun
			 *  @description: Allows a player to interact with an imaginary currency
			 */
			if (split[0].equalsIgnoreCase("/money")) {
				// Level 1, /money
				if ((split.length < 2)) {
					//-------------------------------------------------------------
					// TIER 1 [SELF MONEY CHECK
					//-------------------------------------------------------------
					p.showBalance(player.getName(), null, true);
					return true;
				}

				// Level 2, [player], top, rank
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("-p") || split[1].equalsIgnoreCase("pay")) {
						//-------------------------------------------------------------
						// TIER 2 [PAY]
						//-------------------------------------------------------------
						if (!p.can(player, "pay")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-p|pay] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-c") || split[1].equalsIgnoreCase("credit")) {
						//-------------------------------------------------------------
						// TIER 2 [CREDIT]
						//-------------------------------------------------------------
						if (!p.can(player, "credit")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-c|credit] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-d") || split[1].equalsIgnoreCase("debit")) {
						//-------------------------------------------------------------
						// TIER 2 [DEBIT]
						//-------------------------------------------------------------
						if (!p.can(player, "debit")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-d|debit] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-x") || split[1].equalsIgnoreCase("reset")) {
						//-------------------------------------------------------------
						// TIER 2 [RESET]
						//-------------------------------------------------------------
						if (!p.can(player, "reset")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-x|reset] <player> <notify(y|n)>");
						return true;
					} else if (split[1].equalsIgnoreCase("-t") || split[1].equalsIgnoreCase("top")) {
						//-------------------------------------------------------------
						// TIER 2 [TOP]
						//-------------------------------------------------------------
						if (!p.can(player, "top")) {
							return false;
						}

						p.top(player, 5);
						return true;
					} else if (split[1].equalsIgnoreCase("-r") || split[1].equalsIgnoreCase("rank")) {
						//-------------------------------------------------------------
						// TIER 2 [RANK]
						//-------------------------------------------------------------
						if (!p.can(player, "rank")) {
							return false;
						}

						p.rank(player.getName(), null, true);
						return true;
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "money");
						return true;
					} else {
						//-------------------------------------------------------------
						// TIER 2 [PLAYER MONEY CHECK]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "view")) {
							return false;
						}

						localPlayer = p.getPlayer(split[1]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						// Show another players balance
						p.showBalance(pName, player, false);
						return true;
					}
				}

				// Level 3, top [amount], rank [amount], debit [amount] (self)
				if ((split.length < 4)) {
					if (split[1].equalsIgnoreCase("-p") || split[1].equalsIgnoreCase("pay")) {
						//-------------------------------------------------------------
						// TIER 3 [PAY]
						//-------------------------------------------------------------
						if (!p.can(player, "pay")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-p|pay] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-c") || split[1].equalsIgnoreCase("credit")) {
						//-------------------------------------------------------------
						// TIER 3 [CREDIT]
						//-------------------------------------------------------------
						if (!p.can(player, "credit")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-c|credit] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-d") || split[1].equalsIgnoreCase("debit")) {
						//-------------------------------------------------------------
						// TIER 3 [DEBIT]
						//-------------------------------------------------------------
						if (!p.can(player, "debit")) {
							return false;
						}

						i = 0;

						try {
							i = Integer.parseInt(split[2]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-d|debit] <player> <amount>");
							return true;
						}

						// Show this amount!
						p.debit(player.getName(), player.getName(), Integer.parseInt(split[2]), true);
						return true;
					} else if (split[1].equalsIgnoreCase("-x") || split[1].equalsIgnoreCase("reset")) {
						//-------------------------------------------------------------
						// TIER 3 [RESET]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "reset")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						p.reset(pName, player, true);
						return true;
					} else if (split[1].equalsIgnoreCase("-t") || split[1].equalsIgnoreCase("top")) {
						//-------------------------------------------------------------
						// TIER 3 [TOP]
						//-------------------------------------------------------------
						if (!p.can(player, "top")) {
							return false;
						}

						i = 0;

						try {
							i = Integer.parseInt(split[2]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							return true;
						}

						// Show this amount!
						p.top(player, i);
						return true;
					} else if (split[1].equalsIgnoreCase("-r") || split[1].equalsIgnoreCase("rank")) {
						//-------------------------------------------------------------
						// TIER 3 [RANK]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "rank")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						// Show another players rank
						rank(pName, player.getName(), false);
						return true;
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "money");
						return true;
					} else {
						player.sendMessage(Colors.Rose + "Usage: /money [command|player] [parameter] [parameter]");
						player.sendMessage(Colors.Rose + "    Commands: pay, credit, debit, top, rank");
						player.sendMessage(Colors.Rose + "Alt-Commands: -p, -c, -d, -t, -r");
						return true;
					}
				}

				// Level 4
				if ((split.length < 5)) {
					if (split[1].equalsIgnoreCase("-p") || split[1].equalsIgnoreCase("pay")) {
						//-------------------------------------------------------------
						// TIER 4 [PAY]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "pay")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						i = 0;

						try {
							i = Integer.parseInt(split[3]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-p|pay] <player> <amount>");
							return true;
						}

						// Pay amount
						p.pay(player.getName(), pName, Integer.parseInt(split[3]));
						return true;
					} else if (split[1].equalsIgnoreCase("-c") || split[1].equalsIgnoreCase("credit")) {
						//-------------------------------------------------------------
						// TIER 4 [CREDIT]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "credit")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						i = 0;

						try {
							i = Integer.parseInt(split[3]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-c|credit] <player> <amount>");
							return true;
						}

						// Credit amount
						p.deposit(player.getName(), pName, Integer.parseInt(split[3]), true);
						return true;
					} else if (split[1].equalsIgnoreCase("-d") || split[1].equalsIgnoreCase("debit")) {
						//-------------------------------------------------------------
						// TIER 4 [DEBIT]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "debit")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						i = 0;

						try {
							i = Integer.parseInt(split[3]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-d|debit] <player> <amount>");
							return true;
						}

						// Show this amount!
						p.debit(player.getName(), pName, Integer.parseInt(split[3]), true);
						return true;
					} else if (split[1].equalsIgnoreCase("-x") || split[1].equalsIgnoreCase("reset")) {
						//-------------------------------------------------------------
						// TIER 4 [RESET]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "reset")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						if (split[3].equalsIgnoreCase("y") || split[3].equalsIgnoreCase("yes")) {
							p.reset(pName, player, true);
							return true;
						} else if (split[3].equalsIgnoreCase("n") || split[3].equalsIgnoreCase("no")) {
							p.reset(pName, player, false);
							return true;
						} else {
							player.sendMessage(Colors.Rose + "Invalid Parameter[3] for /shop reset. must be y/n");
							return true;
						}
					} else if (split[1].equalsIgnoreCase("-t") || split[1].equalsIgnoreCase("top")) {
						//-------------------------------------------------------------
						// TIER 4 [TOP]
						//-------------------------------------------------------------
						if (!p.can(player, "top")) {
							return false;
						}

						i = 0;

						try {
							i = Integer.parseInt(split[2]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-t|top] <amount>");
							return true;
						}

						// Show this amount!
						p.top(player, Integer.parseInt(split[2]));
						return true;
					} else if (split[1].equalsIgnoreCase("-r") || split[1].equalsIgnoreCase("rank")) {
						//-------------------------------------------------------------
						// TIER 4 [RANK]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "rank")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						// Show another players rank
						rank(pName, player.getName(), false);
						return true;
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "money");
						return true;
					} else {
						player.sendMessage(Colors.Rose + "Usage: /money [command|player] [parameter] [parameter]");
						player.sendMessage(Colors.Rose + "    Commands: pay, credit, debit, top, rank");
						player.sendMessage(Colors.Rose + "Alt-Commands: -p, -c, -d, -t, -r");
						return true;
					}
				}
				return true;
			}

			/*
			 *  iConomy [Shop] [Basic 1.0]
			 *
			 *  @author: Nijikokun
			 *  @description: Creates a basic shop!
			 *
			 *  @commands:
			 *	/auction -s|start <time-seconds> <item> <amount> <start-bid> [min-bid] [max-bid] - starts the auction with name for concurrent bids
			 *	/auction -s 30 stone 20
			 *	/auction -b|bid <a> - bid on the current auction
			 *	/auction -e|end - end the auction
			 *	/auction ?|help - help documentation
			 *	/auction - Lists currenct auction details
			 */
			if (split[0].equalsIgnoreCase("/auction") && p.globalAuction) {
				if ((split.length < 2)) {
					if(p.auctionTimerRunning) {
						String itemName = (String) p.items.get(cInt(p.auctionItem));
						itemName.replace("-", " ");
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+ Colors.Yellow + " Started by " + Colors.Gold + p.auctionStarter + Colors.Yellow + " Currently Winning: " + Colors.Gold + ((p.auctionCurName == null) ? "Nobody" : p.auctionCurName));
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Item: ["+Colors.LightGray + p.auctionAmount + Colors.Yellow + "] " + Colors.LightGray + itemName);
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Starting Bid: " + Colors.LightGray + p.auctionStartingBid);
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Current Balance: " + Colors.LightGray + p.auctionCurAmount);

					} else {
						player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction ?"+Colors.Rose+" to learn more!");
					}

					return true;
				}

				// Level 2
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "auction");
						return true;
					} else if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("bid")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							player.sendMessage(Colors.Rose + "Cannot bid on your own auction!");
						} else if (p.can(player, "bid")) {
							player.sendMessage(Colors.Rose + "Usage: /auction bid <amount>");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b");
						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-e") || split[1].equalsIgnoreCase("end")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							p.endAuction();
						} else if (p.can(player, "end")) {
							p.endAuction();
						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("start")) {
						player.sendMessage(Colors.Rose + "Usage: /auction start <time-seconds> <item> <amount> <start-bid>");
						player.sendMessage(Colors.Rose + "    Optional after start-bid: min-bid, max-bid");
						player.sendMessage(Colors.Rose + "Alt-Commands: -s");
						return true;
					}

					return true;
				}

				// Level 3
				if ((split.length < 4)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "auction");
						return true;
					} else if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("bid")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							player.sendMessage(Colors.Rose + "Cannot bid on your own auction!");
						} else if (p.can(player, "bid")) {
							int amount = Integer.parseInt(split[2]);

							if(amount < p.auctionCurAmount) {
								player.sendMessage(Colors.Rose + "You must bid at least over "+p.auctionCurAmount+p.moneyName+"!");
								return true;
							}

							if(amount > p.getBalance(player)) {
								player.sendMessage(Colors.Rose + "You cannot bid more than you have!");
								p.showBalance(player.getName(), player, true);
								return true;
							}

							p.bidAuction(player, amount, 0);

						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-e") || split[1].equalsIgnoreCase("end")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							p.endAuction();
						} else if (p.can(player, "end")) {
							p.endAuction();
						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("start")) {
						player.sendMessage(Colors.Rose + "Usage: /auction start <time-seconds> <item> <amount> <start-bid>");
						player.sendMessage(Colors.Rose + "    Optional after start-bid: min-bid, max-bid");
						player.sendMessage(Colors.Rose + "Alt-Commands: -s");
						return true;
					}

					return true;
				}

				if ((split.length < 10)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "auction");
					} else if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("bid")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							player.sendMessage(Colors.Rose + "Cannot bid on your own auction!");
						} else if (p.can(player, "bid")) {
							int amount = Integer.parseInt(split[2]);
							int secret = 0;

							if(amount < p.auctionCurAmount) {
								player.sendMessage(Colors.Rose + "You must bid at least over "+p.auctionCurAmount+p.moneyName+"!");
								return true;
							}

							if(amount > p.getBalance(player)) {
								player.sendMessage(Colors.Rose + "You cannot bid more than you have!");
								p.showBalance(player.getName(), player, true);
								return true;
							}

							if(split.length < 5) {
								secret = Integer.parseInt(split[3]);
							}

							p.bidAuction(player, amount, secret);

						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-e") || split[1].equalsIgnoreCase("end")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							p.endAuction();
						} else if (p.can(player, "end")) {
							p.endAuction();
						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("start")) {
						int max = 0;
						int min = 0;
						int start = 0;
						int amount = 0;
						int itemID = 0;
						int interval = 0;
						String itemName = "";

						if (!p.can(player, "auction")) {
							return false;
						}

						if(p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(split.length < 5) {
							player.sendMessage(Colors.Rose + "Usage: /auction start <time-seconds> <item> <amount> <start-bid>");
							player.sendMessage(Colors.Rose + "    Optional after start-bid: min-bid, max-bid");
							player.sendMessage(Colors.Rose + "Alt-Commands: -s");
							return true;
						}

						// 6
						if(split.length > 5) {
							interval = Integer.parseInt(split[2]);
							amount = Integer.parseInt(split[4]);
							start = Integer.parseInt(split[5]);

							if(interval < 11) {
								player.sendMessage(Colors.Rose + "Interval must be above 10 seconds!");
								return true;
							}

							try {
								itemID = Integer.parseInt(split[3]);
							} catch (NumberFormatException n) {
								itemID = etc.getDataSource().getItem(split[3]);
							}

							if(itemID < 0) {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}

							if (!Item.isValidItem(itemID)) {
								if(p.items.getKey(split[3]) != null) {
									itemID = Integer.parseInt(p.items.getKey(split[3]).toString());
								} else {
									player.sendMessage(Colors.Rose + "Invalid item!");
									return true;
								}
							}

							itemName = (String) p.items.get(cInt(itemID));
							itemName.replace("-", " ");

							if(amount < 1) {
								player.sendMessage(Colors.Rose + "Amount cannot be lower than 1!");
								return true;
							}
						}

						// 7
						if(split.length >= 7) {
							min = Integer.parseInt(split[8]);

							if(min < 3) {
								player.sendMessage(Colors.Rose + "Min cannot be lower than 2!");
								return true;
							}
						}

						// 9
						if(split.length >= 8) {
							max = Integer.parseInt(split[7]);

							if(max < 4) {
								player.sendMessage(Colors.Rose + "Max cannot be lower than 3!");
								return true;
							} else if(max < min) {
								player.sendMessage(Colors.Rose + "Max cannot be lower than minimum bid!");
								return true;
							}
						}

						if(debugging)
							log.info("[iConomy Debugging] ["+player+"] ["+interval+"] ["+itemID+"] ["+amount+"] ["+start+"] ["+min+"] ["+max+"] [#20361]");

						if(p.startAuction(player, interval, itemID, amount, start, min, max)) {
							player.sendMessage(Colors.Yellow + "Auction has begun.");
							p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " started a new auction.");
							p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Item: ["+Colors.LightGray + amount + Colors.Yellow + "] " + Colors.LightGray + itemName);
							p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Starting Bid: " + Colors.LightGray + start);
							return true;
						} else {
							return true;
						}
					}

					return true;
				}
			}

			/*
			 *  iConomy [Lottery]
			 *
			 *  @author: Nijikokun
			 *  @description: Creates a basic lottery!
			 *
			 *  @commands:
			 *	/lottery - Lets do a lottery
			 */
			if (split[0].equalsIgnoreCase("/lottery") && p.globalLottery) {
				if ((split.length < 2)) {
					if (!p.can(player, "lottery")) {
						return false;
					}

					// Do the lottery!
					p.lottery(player); return true;
				}
				// Level 2
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "lottery");
						return true;
					}
				}
			}

			/*
			 *  iConomy [SignShops] [Basic 1.0]
			 *
			 *  @author: Nijikokun
			 *  @description: Creates a basic shop!
			 *
			 *  @commands:
			 *	/sign - Displays your current stock!
			 */
			if (split[0].equalsIgnoreCase("/sign") && p.globalSigns) {
				if ((split.length < 2)) {
					if(p.signOwnUse)
						player.sendMessage("You currently own " + p.ownSign(player.getName()) + " out of " + p.canOwn(player.getName()) + " possible shops.");

					p.signStock(player);
					return true;
				}
				// Level 2
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("upgrade") || split[1].equalsIgnoreCase("-u")) {
						p.upgradeOwn(player.getName()); return true;
					}
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "sign");
						return true;
					}
				}
				// Level 3
				if ((split.length < 4)) {
					if (split[1].equalsIgnoreCase("stock") || split[1].equalsIgnoreCase("-s")) {
						player.sendMessage(Colors.Rose + "Usage: /sign stock <item> <amount>");
						player.sendMessage(Colors.Rose + "Alt-Commands: -s");
						return true;
					}

					if (split[1].equalsIgnoreCase("empty") || split[1].equalsIgnoreCase("-e")) {
						int itemID = 0;

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString()); return true;
							} else {
								player.sendMessage(p.signInvalidItem); return true;
							}
						}

						if(p.existsSign(player.getName(), itemID)) {
							int curStock = p.signCurStock(player.getName(), itemID);
							p.signPull(player, player.getName(), itemID, curStock, true, 0); return true;
						} else {
							player.sendMessage(p.signNoExists); return true;
						}
					}

					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "sign");
						return true;
					}
				}

				// Level 3
				if ((split.length < 5)) {
					if (split[1].equalsIgnoreCase("stock") || split[1].equalsIgnoreCase("-s")) {
						int itemID = 0;
						int amount = 0;

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString()); return true;
							} else {
								player.sendMessage(p.signInvalidItem); return true;
							}
						}

						try {
							amount = Integer.parseInt(split[3]);
						} catch (NumberFormatException n) { }

						if(p.existsSign(player.getName(), itemID) && amount > 0) {
							p.signPush(player, player.getName(), itemID, amount, true, 0, null); return true;
						} else {
							player.sendMessage(p.signNoExists); return true;
						}
					}

					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "sign");
						return true;
					}
				}
			}

			/*
			 *  iConomy [Shop] [Basic 1.0]
			 *
			 *  @author: Nijikokun
			 *  @description: Creates a basic shop!
			 *
			 *  @commands:
			 *	/shop <i> - Displays amount per 1 item
			 *	/shop <i> <a> - Displays amount per <a> items
			 *	/shop -b|buy <i> <a> - Purchase the item
			 *	/shop -s|sell <i> <a> - Sell an item
			 */
			if (split[0].equalsIgnoreCase("/shop") && p.globalShop) {
				// Level 1
				if ((split.length < 2)) {
					player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
					player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
					player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
					return true;
				}

				// Level 2
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "shop");
						return true;
					} else {
						int itemID = 0;

						try {
							itemID = Integer.parseInt(split[1]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[1]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[1]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[1]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						if (itemID != 0) {
							int bNA = p.itemNeedsAmount("buy", cInt(itemID));
							int sNA = p.itemNeedsAmount("sell", cInt(itemID));

							int buying = p.itemCost("buy", cInt(itemID), bNA, false);
							int selling = p.itemCost("sell", cInt(itemID), sNA, false);

							if (buying != 0) {
								if (bNA > 1) {
									player.sendMessage(" " + Colors.Green + "Must be " + Colors.Green + "bought" + Colors.LightGray + " in bundles of " + Colors.Green + bNA + Colors.LightGray + " for " + Colors.Green + buying + p.moneyName + Colors.LightGray + ".");
								} else {
									player.sendMessage(Colors.LightGray + "Can be " + Colors.Green + "bought" + Colors.LightGray + " for " + Colors.Green + buying + p.moneyName + Colors.LightGray + ".");
								}
							} else {
								player.sendMessage(Colors.Rose + "Currently not for purchasing.");
							}

							if (selling != 0) {
								if (sNA > 1) {
									player.sendMessage(Colors.LightGray + "Must be " + Colors.Green + "sold" + Colors.LightGray + " in bundles of " + Colors.Green + sNA + Colors.LightGray + " for " + Colors.Green + selling + p.moneyName + ".");
								} else {
									player.sendMessage(Colors.LightGray + "Can be " + Colors.Green + "sold" + Colors.LightGray + " for " + Colors.Green + selling + p.moneyName + Colors.LightGray + ".");
								}
							} else {
								player.sendMessage(Colors.Rose + "Currently cannot be sold.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: list, buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -l, -b, -s, ?");
							return true;
						}
					}
				}

				// Level 3
				if ((split.length < 4)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "shop");
						return true;
					} else if(split[1].equalsIgnoreCase("-l") || split[1].equalsIgnoreCase("list")) {
						String type = split[2];

						if(type.equalsIgnoreCase("-b") || type.equalsIgnoreCase("buy")) {
							p.showBuyersList(player, 1);
						} else if(type.equalsIgnoreCase("-s") || type.equalsIgnoreCase("sell")) {
							p.showSellersList(player, 1);
						} else {
							player.sendMessage(Colors.Rose + "Invalid Usage: /shop list [buy|sell] <page>");
							player.sendMessage(Colors.Rose + "Alt-Usage: -l, -b|-s");
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("buy")) {
						int itemID = 0;

						if (!p.can(player, "buy")) {
							return false;
						}

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						if (itemID != 0) {
							int buying = p.itemNeedsAmount("buy", cInt(itemID));

							if (buying != 0) {
								p.doPurchase(player, itemID, 1);
							} else {
								player.sendMessage(Colors.Rose + "Item currently not for purchasing.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("sell")) {
						int itemID = 0;

						if (!p.can(player, "sell")) {
							return false;
						}

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						if (itemID != 0) {
							int selling = p.itemNeedsAmount("sell", cInt(itemID));

							if (selling != 0) {
								p.doSell(player, itemID, 1);
							} else {
								player.sendMessage(Colors.Rose + "Currently cannot be sold.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "shop");
						return true;
					} else {
						int itemID = 0;
						int amount = Integer.parseInt(split[2]);

						try {
							itemID = Integer.parseInt(split[1]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[1]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID) && amount == 0) {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						} else if(!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[1]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[1]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
								player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
								player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
								return true;
							}
						}

						if (itemID != 0) {
							int bNA = p.itemNeedsAmount("buy", cInt(itemID));
							int sNA = p.itemNeedsAmount("sell", cInt(itemID));

							int buying = p.itemCost("buy", cInt(itemID), amount, false);
							int selling = p.itemCost("sell", cInt(itemID), amount, false);
							int totalBuying = p.itemCost("buy", cInt(itemID), amount, true);
							int totalSelling = p.itemCost("sell", cInt(itemID), amount, true);

							if (buying != 0) {
								if (bNA > 1) {
									player.sendMessage(Colors.Green + amount + Colors.LightGray + " bundles will cost " + Colors.Green + totalBuying + p.moneyName + Colors.LightGray + ".");
								} else {
									player.sendMessage(Colors.Green + amount + Colors.LightGray + " will cost " + Colors.Green + totalBuying + p.moneyName + Colors.LightGray + ".");
								}
							} else {
								player.sendMessage(Colors.Rose + "Invalid amount or not for purchasing!");
							}

							if (selling != 0) {
								if (sNA > 1) {
									player.sendMessage(Colors.Green + amount + Colors.LightGray + " bundles will sell for " + Colors.Green + totalSelling + p.moneyName + Colors.LightGray + ".");
								} else {
									player.sendMessage(Colors.Green + amount + Colors.LightGray + " can be sold for " + Colors.Green + totalSelling + p.moneyName + Colors.LightGray + ".");
								}
							} else {
								player.sendMessage(Colors.Rose + "Invalid Amount or not for selling!");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					}
				}

				// Level 4
				if ((split.length < 5)) {
					if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("buy")) {
						int itemID = 0;

						if (!p.can(player, "buy")) {
							return false;
						}

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						int amount = Integer.parseInt(split[3]);

						if (amount < 0 || amount == 0) {
							player.sendMessage(Colors.Rose + "Invalid amount!");
							return true;
						}

						if (itemID != 0) {
							int buying = p.itemNeedsAmount("buy", cInt(itemID));

							if (buying != 0) {
								p.doPurchase(player, itemID, amount);
							} else {
								player.sendMessage(Colors.Rose + "Item currently not for purchasing.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					} else if(split[1].equalsIgnoreCase("-l") || split[1].equalsIgnoreCase("list")) {
						String type = split[2];

						int amount = Integer.parseInt(split[3]);

						if (amount < 2 || amount == 1) {
							amount = 1;
						}

						if(type.equalsIgnoreCase("-b") || type.equalsIgnoreCase("buy")) {
							p.showBuyersList(player, amount);
						} else if(type.equalsIgnoreCase("-s") || type.equalsIgnoreCase("sell")) {
							p.showSellersList(player, amount);
						} else {
							player.sendMessage(Colors.Rose + "Invalid Usage: /shop list [buy|sell] <page>");
							player.sendMessage(Colors.Rose + "Alt-Usage: -l, -b|-s");
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("sell")) {
						int itemID = 0;

						if (!p.can(player, "sell")) {
							return false;
						}

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						int amount = Integer.parseInt(split[3]);

						if (amount < 0 || amount == 0) {
							player.sendMessage(Colors.Rose + "Invalid amount!");
							return true;
						}

						if (itemID != 0) {
							int selling = p.itemNeedsAmount("sell", cInt(itemID));

							if (selling != 0) {
								p.doSell(player, itemID, amount);
							} else {
								player.sendMessage(Colors.Rose + "Item currently not for selling.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "shop");
						return true;
					} else {
						player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
						player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
						player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
						return true;
					}
				}
			}
			return false;
		}

		public boolean onComplexBlockChange(Player player, ComplexBlock block) {
			if(!p.globalSigns)
				return false;

			if (!p.can(player, "sign") || !p.can(player, "trade")) {
				return false;
			}

			if (!(block instanceof Sign)) {
				return false;
			}

			Sign sign = (Sign) block;

			// Trading
			if(sign.getText(1).equalsIgnoreCase("[trade]")) {
				sign.setText(2, "Setup Chest");
				sign.setText(3, "Reclick Sign");
				sign.update();
				return false;
			}

			// Selling / Buying
			if (!(sign.getText(1).equalsIgnoreCase("sell") || sign.getText(1).equalsIgnoreCase("buy"))) {
				return false;
			}

			if(p.signOwnUse){
				if(p.ownSign(player.getName())+1 > p.canOwn(player.getName())){
					log.info("[iConomy SS] Cannot create shop Own:"+p.ownSign(player.getName())+" Can:"+p.canOwn(player.getName()));
					return false;
				}
			}

			String[] split = sign.getText(2).split(" ");

			if (split.length != 2) {
				player.sendMessage(String.format(p.signFailedToParse, sign.getText(2)));
				sign.setText(0, ""); return false;
			}

			int amount, perTransaction, item = Integer.parseInt(split[0]);

			try {
				amount = Integer.parseInt(split[1]);

				if (amount <= 0) {
					player.sendMessage(String.format(p.signAmountGreaterThan, 0));
					sign.setText(0, ""); return false;
				}
			} catch (Exception ex) {
				player.sendMessage(String.format(p.signValuesNotNumerical, sign.getText(2))); return false;
			}

			if (!Item.isValidItem(item)) {
				if(p.items.getKey(split[0]) != null) {
					item = Integer.parseInt(p.items.getKey(split[0]).toString());
				} else {
					player.sendMessage(String.format(p.signInvalidItem)); return true;
				}
			}

			if (amount > 64) {
				player.sendMessage(String.format(p.signMaxBuySell, 64)); return false;
			}

			try {
				perTransaction = Integer.parseInt(sign.getText(3));

				if (perTransaction <= 0) {
					player.sendMessage(String.format(p.signTransactionGreaterThan, 0));
					sign.setText(0, ""); return false;
				}
			} catch (Exception ex) {
				player.sendMessage(String.format(p.signValuesNotNumerical, sign.getText(2))); return false;
			}

			sign.setText(0, player.getName());

			if(sign.getText(1).equalsIgnoreCase("sell")) {
				player.sendMessage(String.format(p.signSellCreated, amount, p.itemName(String.valueOf(item)), sign.getText(3) + p.moneyName));
			} else {
				player.sendMessage(String.format(p.signBuyCreated, amount, p.itemName(String.valueOf(item)), sign.getText(3) + p.moneyName));
			}

			p.setSign(player.getName(), sign.getX(), sign.getY(), sign.getZ(), item, 0);
			return false;
		}

		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {

			if (itemInHand != -1) {
				return false;
			}

			if(!p.globalSigns)
				return false;

			if (!p.can(player, "signBuy") || !p.can(player, "signSell") || !p.can(player, "trade")) {
				return false;
			}

			ComplexBlock theblock = etc.getServer().getComplexBlock(blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());

			if (!(theblock instanceof Sign)) {
				return false;
			} else {
				Sign sign = (Sign) theblock;

				// Trading
				if(sign.getText(1).equalsIgnoreCase("[trade]")) {
					Chest chest = null;
					Item item = null;
					Map traders = null;

					String name = "";
					int amount = 0, give = 0, toGive = 0, itemId = 0, remainder = 0, factor = 0, items = 0, i = 0;
					int x, y, z;

					//Coordinates of the sign
					x = blockClicked.getX();
					y = blockClicked.getY();
					z = blockClicked.getZ();

					ComplexBlock theOBlock = etc.getServer().getComplexBlock(x,y-1,z);

					if(theOBlock instanceof Chest)
						chest = (Chest) etc.getServer().getComplexBlock(x, y-1, z);

					//Is there something in the first slot?
					if (chest.getEmptySlot() != 0 ) {
						item = chest.getItemFromSlot(0);
						amount = item.getAmount();

						factor = 0;
						items = 0;
						i = 1;

						try {
							traders = p.trades.returnMap();
						} catch (Exception ex) {
							log.info("[iConomy] Mapping failed for trades"); return false;
						}

						for (Object key: traders.keySet()) {
							String data = (String) traders.get(key);
							String tradeData[] = data.split(",");

							if(Integer.parseInt(tradeData[0]) == item.getItemId()) {
								factor = Integer.parseInt(tradeData[1]);
								name = (String) key;
								toGive = Integer.parseInt(tradeData[2]);

								if(tradeData.length == 4)
									itemId = Integer.parseInt(tradeData[3]);

								items = i;
								break;
							}

							i++;
						}

						player.sendMessage("§6[Trade]§f Trading: " + name + " - Amount: " + amount + ".");

						//Calculates the amount of gold (or the currency) to give the player
						if (factor != 0) {
							if (amount >= factor) {
								give = amount / factor;
								remainder = amount % factor;

								//Send the "money" to the player
								if(itemId != 0)
									player.giveItem(itemId, give * toGive);
								else {
									p.deposit(null, player.getName(), give * toGive, true);
								}

								//Removes the content's of the chest and add the remainder, if any
								item.setAmount(remainder);
								chest.removeItem(0);
								chest.addItem(item);

								//Let you and everybody know what happened...
								if(itemId != 0)
									player.sendMessage("§6[Trade]§f You received " + (give * toGive) + " " + p.itemName(cInt(itemId)) + ".");
								else {
									player.sendMessage("§6[Trade]§f You received " + (give * toGive) + p.moneyName + ".");
								}

								player.sendMessage("§6[Trade]§f There are " + remainder + " " + name + " left in the chest.");
								if(itemId != 0)
									p.broadcast("§6[Trade] " + player.getName() + " got " + (give * toGive) + " " + p.itemName(cInt(itemId)) + " from trade.");
								else {
									p.broadcast("§6[Trade] " + player.getName() + " got " + (give * toGive) + p.moneyName + " from trade.");
								}
							} else {
								player.sendMessage("§6[Trade]§f Not enough items (" + amount + "/" + factor + ") for trade.");
							}
						} else {
							player.sendMessage("§6[Trade]§f This item cannot be used for trade.");
						}

						//Update the chest content
						chest.update();
					} else {
						player.sendMessage("§6[Trade]§f Add the items into the first slot.");
					}

					return false;
				}

				// Trade Recursive Information~
				if((sign.getText(1).equalsIgnoreCase("[rates]"))) {
					Map traders;
					player.sendMessage("§6[Trade]§f Trading rates are currently:");

					try {
						traders = p.trades.returnMap();
					} catch (Exception ex) {
						log.info("[iConomy] Mapping failed for trades"); return false;
					}

					for (Object key: traders.keySet()) {
						String data = (String) traders.get(key);
						String tradeData[] = data.split(",");

						if(tradeData.length == 4)
							player.sendMessage("    §6-§f " + tradeData[1] + "x " + (String) key + " for " + tradeData[2] + " " + p.itemName(tradeData[3]));
						else {
							player.sendMessage("    §6-§f " + tradeData[1] + "x " + (String) key + " for " + tradeData[2] + p.moneyName);
						}
					}
				}

				// Selling / Buying
				if (!(sign.getText(1).equalsIgnoreCase("sell") || sign.getText(1).equalsIgnoreCase("buy"))) {
					return false;
				}

				player.addGroup("groupname");


				if (!p.waitedEnough(player.getName())) {
					player.sendMessage(String.format(p.signWait, p.signWaitAmount));
					return false;
				}

				p.updateClick(player.getName());

				String[] split = sign.getText(2).split(" ");
				int i = Integer.parseInt(split[0]);
				int amount = Integer.parseInt(split[1]);
				int price = Integer.parseInt(sign.getText(3));

				if (amount <= 0) {
					player.sendMessage(String.format(p.signAmountGreaterThan, 0));
					sign.setText(0, ""); sign.setText(1, ""); sign.setText(2, ""); sign.setText(3, "");
					sign.update();
					return false;
				}

				if(!p.existsSign(sign.getText(0), i)) {
					player.sendMessage(p.signInvalid);
					sign.setText(0, ""); sign.setText(1, ""); sign.setText(2, ""); sign.setText(3, "");
					sign.update();
					return false;
				}

				int curStock = p.signCurStock(sign.getText(0), i);

				if (player.getName().equalsIgnoreCase(sign.getText(0))) {
					if (sign.getText(1).equalsIgnoreCase("sell")) {
						p.signPush(player, sign.getText(0), i, amount, true, 0, blockClicked);
					} else {
						if(curStock < 1) {
							player.sendMessage(String.format(p.signNotInStocky, "You", p.itemName(cInt(i))));
							return false;
						}

						p.signPull(player, sign.getText(0), i, amount, true, 0);
					}
				} else {
					if (sign.getText(1).equalsIgnoreCase("sell")) {
						if (!p.canAfford(sign.getText(0), price)) {
							player.sendMessage(String.format(p.signOwnerBankrupt, "Owner", p.moneyName));
							return false;
						} else {
							p.signPush(player, sign.getText(0), i, amount, false, price, blockClicked);
						}
					} else {
						if (!p.canAfford(player.getName(), price)) {
							player.sendMessage(String.format(p.signNotEnoughp, p.moneyName));
							return false;
						} else {

							if(curStock < 1) {
								player.sendMessage(String.format(p.signNotInStock, sign.getText(0), p.itemName(cInt(i))));
								return false;
							}

							p.signPull(player, sign.getText(0), i, amount, false, price);
						}
					}

				}
			}
			return false;
		}

		public boolean onBlockDestroy(Player player, Block block) {

			if(!p.globalSigns)
				return false;

			ComplexBlock theblock = etc.getServer().getComplexBlock(block.getX(), block.getY(), block.getZ());

			if (!(theblock instanceof Sign)) {
				return false;
			} else {
				Sign sign = (Sign) theblock;

				if (!(sign.getText(1).equalsIgnoreCase("sell") || sign.getText(1).equalsIgnoreCase("buy"))) {
					return false;
				}

				String[] split = sign.getText(2).split(" ");
				int i = Integer.parseInt(split[0]);

				if(!p.existsSign(sign.getText(0), i)) {
					return false;
				}

				if(!sign.getText(0).equalsIgnoreCase(player.getName())){
					if(block.getStatus() == 0 || block.getStatus() == 1 || block.getStatus() == 3) {
						player.sendMessage("This sign is protected.");
						return true;
					}
				} else {
					if(block.getStatus() == 3){
						p.deleteSign(player.getName(), i);
						sign.setText(0, ""); sign.setText(1, ""); sign.setText(2, ""); sign.setText(3, "");
						sign.update();
						return false;
					}
				}
			}

			return false;
		}
	}

	class ValueComparator implements Comparator {

	  Map base;
	  public ValueComparator(Map base) {
	      this.base = base;
	  }

	  public int compare(Object a, Object b) {
		int ax = Integer.parseInt((String)base.get(a));
		int bx = Integer.parseInt((String)base.get(b));

	    if(ax < bx) {
	      return 1;
	    } else if(ax == bx) {
	      return 0;
	    } else {
	      return -1;
	    }
	  }
	}
}
