package com.tower.game.api;

import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.Item;
import com.tower.game.service.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class TestController {


    @Autowired
    private ItemService itemService;

    @GetMapping("/check-thread")
    public String checkThread() {
        // 打印当前线程的信息
        System.out.println(Thread.currentThread());
        return "请查看控制台输出";
    }

    @GetMapping("/io-task")
    public ApiResponse ioTask() throws InterruptedException {
        // 模拟I/O操作，比如查询数据库或调用外部API
//        Thread.sleep(50); // 阻塞50毫秒
//        log.info("任务完成");


        List<Item> list = itemService.listByIds(List.of(1));
        return ApiResponse.success(null);
    }



}