import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;

import org.joda.time.DateTime;

class TaskHandler {
	private static final String MESSAGE_ADD_EMPTY = "Error, please add a task description.\n";
	private static final String MESSAGE_ADDED_TASK = "Added \"%s\".\n";
	private static final String MESSAGE_EMPTY_TASKS = "You have no tasks scheduled.\n";
	private static final String MESSAGE_UPDATE_TASK = "Task has been updated.\n";
	private static final String MESSAGE_UPDATE_NO_SUCH_TASK = "Error, please enter a valid task number to update.\n";
	private static final String MESSAGE_UPDATE_ARGUMENT_ERROR = "Error, incorrect update format.\n";
	private static final String MESSAGE_LIST_NUMBER = "%d. %s";
	private static final String MESSAGE_DELETE_ARGUMENT_ERROR = "Error, incorrect delete format.\n";
	private static final String MESSAGE_TASK_DELETED = "\"%s\" has been deleted from the task list.\n";
	private static final String MESSAGE_TASK_DELETED_ALL = "All tasks have been deleted from the task list.\n";
	private static final String MESSAGE_INVALID_DELETE = "No such task, please enter a valid number to delete.\n";
	
	private static final String MINUTE_LAST = "23:59";
	private static final String MINUTE_FIRST = "00:00";
	
	/**
	 * Add a task using the user input
	 * @param userInput
	 * @return a Feedback object to be shown to the user
	 */
	protected static Feedback addTask(String userInput) {
		if (!CommandParser.isInputValid(userInput, 1)) {
			return new Feedback(MESSAGE_ADD_EMPTY);
		}
		
		Task newTask = createTask(userInput);
		
		HistoryHandler.pushUndoStack();
		Task.getList().add(newTask);
		Task.sortList();
		Task.saveTasks();
		HistoryHandler.purgeRedoStack();
		return new Feedback(String.format(MESSAGE_ADDED_TASK, userInput));
	}
	
	
	protected static Task createTask(String userInput) {
		String[] inputTokens = userInput.split("\\s+");
		ArrayList<String> input = new ArrayList<String>(Arrays.asList(inputTokens));
		
		String alias = CommandParser.getAliasFromDescription(userInput);		
		alias = (Task.isAliasValid(alias) || isInteger(alias)) ? null : alias;
		userInput = CommandParser.removeDateTimeFromString(userInput);
		userInput = CommandParser.removeAliasFromDescription(userInput);
		
		String[] fields = CommandParser.getTaskFields(input);
		Task newTask = createTaskFromFields(fields, userInput, alias);
		
		return newTask;
	}
	
	/**
	 * Reads the user input and time fields and creates a Task object
	 * @param fields
	 * @param input
	 * @return Task object	
	 */
	private static Task createTaskFromFields(String[] fields, String input, String alias) {
		DateTime start = null;
		DateTime end = null;
	
		if (fields[Task.START_DATE] == null) {
			if (fields[Task.START_TIME] != null) {
				start = DateParser.setDate();
				start = TimeParser.setTime(start, fields[Task.START_TIME]);
				
				// If null, "Deadline" Task with today's date
				// Else, Timed Task with today's date
				if (fields[Task.END_TIME] != null) {
					end = DateParser.setDate();
					end = TimeParser.setTime(start, fields[Task.END_TIME]);
				} 
			}
		} else {
			start = DateParser.setDate(fields[Task.START_DATE]);
			
			if (fields[Task.START_TIME] == null) {
				if (fields[Task.END_DATE] == null) {
					// "Deadline" Task with only start date
					start = TimeParser.setTime(start, MINUTE_LAST);
				} else {
					// Timed Task with start and end dates but no time
					start = TimeParser.setTime(start, MINUTE_FIRST);
					end = DateParser.setDate(fields[Task.END_DATE]);
					end = TimeParser.setTime(end, MINUTE_LAST);
				}
				
			} else {
				start = TimeParser.setTime(start, fields[Task.START_TIME]);
				
				if (fields[Task.END_DATE] == null) {
					if (fields[Task.END_TIME] != null) {
						// Timed Task with start and end times within one specific date
						end = DateParser.setDate(fields[Task.START_DATE]);
						end = TimeParser.setTime(end, fields[Task.END_TIME]);
					}
				} else {
					end = DateParser.setDate(fields[Task.END_DATE]);
					
					if (fields[Task.END_TIME] == null) {
						// Timed Task with start time, start and end dates, but no end time
						end = TimeParser.setTime(end, MINUTE_LAST);
					} else {
						// Timed Task with start and end time/dates
						end = TimeParser.setTime(end, fields[Task.END_TIME]);
					}
				}
			}
		}

		return new Task(input, start, end, alias);
	}
	
	/**
	 * Updates a task in the specified field
	 * Input format is "[task number/alias][field to update][update]
	 * @param update
	 * @return a Feedback object to be shown to the user
	 */
	protected static Feedback updateTask(String update) {	
		if (!CommandParser.isInputValid(update, 2)) {
			return new Feedback(MESSAGE_UPDATE_ARGUMENT_ERROR);
		}
		
		String taskID = CommandParser.getUserCommandType(update);
		String updateStringWithoutID = CommandParser.getUserCommandDesc(update);
		String updateField = CommandParser.getUserCommandType(updateStringWithoutID);
		String updateDesc = CommandParser.getUserCommandDesc(updateStringWithoutID);
		
		Task taskToUpdate = getTaskToUpdate(taskID);
		
		if (taskToUpdate == null) {
			return new Feedback(MESSAGE_UPDATE_NO_SUCH_TASK);
		}
		
		if (updateField.equalsIgnoreCase("start") || updateField.equalsIgnoreCase("end") || updateField.equalsIgnoreCase("time")) {
			if (!CommandParser.isInputValid(updateDesc, 1)) {
				return new Feedback(MESSAGE_UPDATE_ARGUMENT_ERROR);
			}
			taskToUpdate = updateTaskTime(taskToUpdate, updateField, updateDesc);
			
		} else if (updateField.equals("alias")) {
			String[] tokens = updateDesc.split("\\s+");
			if (tokens.length <= 0) {
				// since the task is previously removed, it needs to be put back in in the event of an error
				Task.getList().add(taskToUpdate);
				return new Feedback("Invalid alias");
			}
			
			String alias = tokens[0];	
			if (Task.isAliasValid(alias) || isInteger(alias)) {
				Task.getList().add(taskToUpdate);
				return new Feedback("Alias is already in use");
			
			} else {
				taskToUpdate.setAlias(alias);
			}
			
		} else if (updateField.equals("desc") || updateField.equals("description")) {
			if (!CommandParser.isInputValid(updateDesc, 1)) {
				return new Feedback(MESSAGE_UPDATE_ARGUMENT_ERROR);
			}
			
			taskToUpdate.setDescription(updateDesc);
		} else {
			taskToUpdate = createTask(updateStringWithoutID);
		}
		
		Task.getList().add(taskToUpdate);
		Task.sortList();
		Task.saveTasks();
		return new Feedback(MESSAGE_UPDATE_TASK);
	}
	
	private static Task updateTaskTime(Task task, String field, String update) {
		ArrayList<String> updateTokens = new ArrayList<String>(Arrays.asList(update.split("\\s+")));
		String[] timeFields = CommandParser.getTaskFields(updateTokens);
		
		if (field.equalsIgnoreCase("time")) {
			Task tempTask = createTask(update);
			task.setStartDateTime(tempTask.getStartDateTime());
			task.setEndDateTime(tempTask.getEndDateTime());
		} else {
			DateTime date = task.getStartDateTime();
			if (timeFields[Task.START_DATE] == null && timeFields[Task.START_TIME] == null) {
				return task;
			} else if (timeFields[Task.START_DATE] == null) {
				date = TimeParser.setTime(date, timeFields[Task.START_TIME]);
			} else if (timeFields[Task.START_TIME] == null) {
				date = DateParser.setDate(timeFields[Task.START_DATE]);
				date = TimeParser.setTime(date, MINUTE_LAST);
			}
			
			if (field.equalsIgnoreCase("start")) {
				task.setStartDateTime(date);
			} else {
				task.setEndDateTime(date);
			}
		}
		
		return task;
	}
	
	private static Task getTaskToUpdate(String taskID) {
		Task taskToUpdate = null;
		int index = -1;
		
		if (isInteger(taskID)) {
			index = Integer.parseInt(taskID) - 1;
		} else if (Task.isAliasValid(taskID)) {
			index = Task.getTaskIndexFromAlias(taskID);
		}
		
		if (index < 0 || index >= Task.getList().size()) {
			return null;
		}
		
		
		taskToUpdate = Task.getList().get(index);
		Task.getList().remove(index);
		
		return taskToUpdate;
	}
	
	/**
	 * Displays all the tasks in order of date
	 * @return a Feedback Object to be shown to the user
	 */
	protected static Feedback listTasks() {
		Task.sortList();
		if (Task.getList().isEmpty()) {
			return new Feedback(MESSAGE_EMPTY_TASKS);
		} else {
			return new Feedback(getListOfTasks(), false);
		}
	}
	
	private static String getListOfTasks() {
		ArrayList<Task> list = Task.loadTasks();
		String stringList = "";
		for (int i = 1; i <= list.size(); i++) {
			stringList += String.format(MESSAGE_LIST_NUMBER, i, list.get(i-1).toDisplayString()) + "\n";
		}
		return stringList;
	}

	/**
	 * Removes a task from the taskList
	 * @param taskID
	 * @return a Feedback object to be shown to the user
	 */
	protected static Feedback deleteTask(String taskID) {
		if (!CommandParser.isInputValid(taskID, 1)) {
			return new Feedback(MESSAGE_DELETE_ARGUMENT_ERROR);
		}
		
		int index = -1;
		
		if (taskID.equalsIgnoreCase("completed")) {
			return new Feedback("Not yet implemented");
		} else {
			if (isInteger(taskID)) {
				index = Integer.parseInt(taskID) - 1;
				if (isOutOfDeleteRange(index)) {
					return new Feedback(MESSAGE_INVALID_DELETE);
				}
			} else if (Task.isAliasValid(taskID)) {
				index = Task.getTaskIndexFromAlias(taskID);
			} 

			if (index == -1) {
				return new Feedback(MESSAGE_INVALID_DELETE);
			}
			
			HistoryHandler.pushUndoStack();
			String deletedTask = Task.getList().get(index).getDescription();
			Task.getList().remove(index);
			Task.saveTasks();
			HistoryHandler.purgeRedoStack();
			return new Feedback(String.format(MESSAGE_TASK_DELETED, deletedTask), false);
		}
	}
	
	private static boolean isOutOfDeleteRange(int index) {
		if (index >= 0 && index < Task.getList().size()) {
			return false;
		} else {
			return true;
		}
	}
	
	private static boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	//search
		
}