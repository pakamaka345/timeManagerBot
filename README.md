# Time Manager Bot

Time Manager Bot is a personal Telegram bot designed to help you manage your weekly schedule. It's built with Java and uses the Telegram Bot API for communication.

## Requirements

- Java 8 or higher
- Maven
- Telegram Bot Token

## Installation

1. Clone the repository to your local machine.
2. Install the required dependencies. This project uses Maven for dependency management.
3. Create a `.env` file in the root directory of the project and add your Telegram Bot Token like this:
    ```
    BOT_TOKEN=Your_Telegram_Bot_Token
    ```
4. Run the `timeManagerBot.java` file to start the bot.

## Configuration

All bot settings are located in the `.env` file. You can change these settings to configure the bot according to your needs.

## Usage

Once the bot is running, you can interact with it on Telegram. Here are some of the commands you can use:

- `/start`: Start the bot and display the main menu.
- `create`: Create a new plan for a specific day.
- `edit`: Edit an existing plan.
- `delete`: Delete an existing plan.
- `show`: Show your plans for a specific day or the entire week.

## Support

If you encounter any problems or have questions, please open an issue in this repository.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License.
