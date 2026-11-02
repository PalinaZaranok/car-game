/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : CarGame Controller
  ******************************************************************************
  */
/* USER CODE END Header */
/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "adc.h"
#include "tim.h"
#include "usart.h"
#include "gpio.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "stdio.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */

/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/

/* USER CODE BEGIN PV */
uint16_t adc_value = 512;
uint8_t forward_btn = 0;
uint8_t backward_btn = 0;

uint32_t red_led_timer = 0;
uint32_t blue_led_timer = 0;
uint32_t buzzer_timer = 0;
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
/* USER CODE BEGIN PFP */
void set_red_led(uint8_t state);
void set_blue_led(uint8_t state);
void buzzer_beep(uint32_t duration_ms);
void buzzer_off(void);
void read_buttons(void);
/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */
void set_red_led(uint8_t state)
{
	HAL_GPIO_WritePin(LED_RED_GPIO_Port, LED_RED_Pin, state ? GPIO_PIN_SET : GPIO_PIN_RESET);
}

void set_blue_led(uint8_t state)
{
	HAL_GPIO_WritePin(LED_BLUE_GPIO_Port, LED_BLUE_Pin, state ? GPIO_PIN_SET : GPIO_PIN_RESET);
}

void buzzer_beep(uint32_t duration_ms)
{
	__HAL_TIM_SET_AUTORELOAD(&htim3, 500 - 1);         
  __HAL_TIM_SET_COMPARE(&htim3, TIM_CHANNEL_1, 250); 
  HAL_TIM_PWM_Start(&htim3, TIM_CHANNEL_1);
  buzzer_timer = HAL_GetTick() + duration_ms;
}

void buzzer_off(void)
{
	HAL_TIM_PWM_Stop(&htim3, TIM_CHANNEL_1);
}

void read_buttons(void)
{
    forward_btn  =	(HAL_GPIO_ReadPin(BTN_FWD_GPIO_Port, BTN_FWD_Pin) == GPIO_PIN_RESET) ? 1 : 0;
    backward_btn =	(HAL_GPIO_ReadPin(BTN_BWD_GPIO_Port, BTN_BWD_Pin) == GPIO_PIN_RESET) ? 1 : 0;
}
/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{

  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_ADC1_Init();
  MX_TIM3_Init();
  MX_USART2_UART_Init();
  /* USER CODE BEGIN 2 */
  set_red_led(0);
  set_blue_led(0);
  buzzer_off();
  /* USER CODE END 2 */

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
    read_buttons();

    HAL_ADC_Start(&hadc1);
    if (HAL_ADC_PollForConversion(&hadc1, 5) == HAL_OK) 
		{
			adc_value = (uint16_t)(HAL_ADC_GetValue(&hadc1) >> 2);
    }

    char tx_buffer[32];
    int len = snprintf(tx_buffer, sizeof(tx_buffer), "S%u,F%u,B%u\n",
                       (unsigned)adc_value,
                       (unsigned)forward_btn,
                       (unsigned)backward_btn);

    if (len > 0)
		{
			HAL_UART_Transmit(&huart2, (uint8_t*)tx_buffer, (uint16_t)len, 20);
    }

    while (__HAL_UART_GET_FLAG(&huart2, UART_FLAG_RXNE) != RESET)
		{
			uint8_t rx_data = (uint8_t)(huart2.Instance->DR & 0xFF);

      if (rx_data == 'C') 
			{
          set_blue_led(1);
          blue_led_timer = HAL_GetTick() + 200;
          buzzer_beep(100);
      }
      else if (rx_data == 'X')
			{
          set_red_led(1);
          red_led_timer = HAL_GetTick() + 500;
          buzzer_beep(300);
      }
    }
		
		if (__HAL_UART_GET_FLAG(&huart2, UART_FLAG_ORE) != RESET) 
		{
			__HAL_UART_CLEAR_OREFLAG(&huart2);
    }

    uint32_t current_tick = HAL_GetTick();

    if (red_led_timer > 0 && current_tick >= red_led_timer) 
		{
			set_red_led(0);
      red_led_timer = 0;
    }
    if (blue_led_timer > 0 && current_tick >= blue_led_timer) 
		{
      set_blue_led(0);
      blue_led_timer = 0;
    }
    if (buzzer_timer > 0 && current_tick >= buzzer_timer)
		{
      buzzer_off();
      buzzer_timer = 0;
    }

    HAL_Delay(15);
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};
  RCC_PeriphCLKInitTypeDef PeriphClkInit = {0};

  /** Initializes the RCC Oscillators according to the specified parameters
  * in the RCC_OscInitTypeDef structure.
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_ON;
  RCC_OscInitStruct.HSEPredivValue = RCC_HSE_PREDIV_DIV1;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
  RCC_OscInitStruct.PLL.PLLMUL = RCC_PLL_MUL9;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_2) != HAL_OK)
  {
    Error_Handler();
  }
  PeriphClkInit.PeriphClockSelection = RCC_PERIPHCLK_ADC;
  PeriphClkInit.AdcClockSelection = RCC_ADCPCLK2_DIV6;
  if (HAL_RCCEx_PeriphCLKConfig(&PeriphClkInit) != HAL_OK)
  {
    Error_Handler();
  }
}

/* USER CODE BEGIN 4 */

/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}
#ifdef USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */
