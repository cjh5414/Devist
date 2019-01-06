package com.tdl.devist.service;


import com.tdl.devist.model.DailyCheck;
import com.tdl.devist.model.Todo;
import com.tdl.devist.model.User;
import com.tdl.devist.repository.DailyCheckRepository;
import com.tdl.devist.repository.TodoRepository;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@Profile("dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TodoServiceTests {

    @Autowired
    private TodoService todoService;

    @Autowired
    private UserService userService;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private DailyCheckRepository dailyCheckRepository;

    private final String TEST_USER_NAME = "dbadmin";
    private final String TEST_TODO_TITLE = "Todo 테스트하기";

    private int entitySize;

    @Test
    @Transactional
    public void 순서1_서비스_레이어에서_Todo_추가_테스트() {
        generateAndSaveTestTodoInstance(TEST_USER_NAME);

        User targetUser = userService.getUserByUserName(TEST_USER_NAME);
        List<Todo> todoList = targetUser.getTodoList();
        entitySize = todoList.size();
        Assert.assertEquals(1, entitySize);
        Todo todo = todoList.get(0);
        Assert.assertEquals(TEST_TODO_TITLE, todo.getTitle());
        Assert.assertEquals(127, todo.getRepeatDay());
    }

    @Test
    @Transactional
    public void 순서2_서비스_레이어에서_Todo_삭제_테스트() {
        generateAndSaveTestTodoInstance(TEST_USER_NAME);
        User user = userService.getUserByUserName(TEST_USER_NAME);
        List<Todo> todoList = user.getTodoList();

        int todoId = todoList.get(0).getId();
        Todo todo = todoService.findTodoById(todoId);
        todoList.remove(todo);

        todoService.deleteTodo(todo);

        User afterUser = userService.getUserByUserName(TEST_USER_NAME);
        Assert.assertNotNull(afterUser);
        List<Todo> AfterTodoList = afterUser.getTodoList();
        Assert.assertEquals(0, AfterTodoList.size());
    }

    private Todo generateAndSaveTestTodoInstance(String username) {
        Todo todo = new Todo();
        User user = userService.getUserByUserName(username);
        todo.setTitle(TEST_TODO_TITLE);
        todo.setRepeatCheckbox(new boolean[]{true, true, true, true, true, true, true});
        todoService.addTodo(user, todo);
        userService.updateUser(user);
        return todo;
    }

    @Test
    @Transactional
    public void testSetTodoIsDone() {
        final int TODO_ID = 0;
        Todo todo = todoRepository.getOne(TODO_ID);
        Assert.assertFalse(todo.isDone());

        todoService.setTodoIsDone(TODO_ID, true);
        todo = todoRepository.getOne(TODO_ID);
        Assert.assertTrue(todo.isDone());

        todoService.setTodoIsDone(TODO_ID, false);
        todo = todoRepository.getOne(TODO_ID);
        Assert.assertFalse(todo.isDone());
    }

    @Test
    @Transactional
    public void testCheckTodosAfterPlanedTime() {
        Todo doneTodo = todoRepository.getOne(3);
        Assert.assertTrue(doneTodo.isDone());
        Assert.assertEquals(0, dailyCheckRepository.findAll().size());

        todoService.checkAndUpdateTodos();

        doneTodo = todoRepository.getOne(3);
        Assert.assertFalse(doneTodo.isDone());

        int todayTodoCount = 0;
        int dayOfWeek = 1 << (LocalDate.now().getDayOfWeek().getValue() - 1);
        for (Todo todo: todoRepository.findAll()) {
            if ((todo.getRepeatDay() & dayOfWeek) > 0)
                todayTodoCount++;
        }

        LocalDate planedDate = LocalDate.now();
        List<DailyCheck> todayDailyCheckList = dailyCheckRepository.findByPlanedDate(planedDate);
        Assert.assertEquals(todayTodoCount, todayDailyCheckList.size());
    }
}