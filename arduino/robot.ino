#include "pitches.h"

// COMMAND NUMBERS
#define COMMAND_FORWARD 1
#define COMMAND_BACKWARDS 2
#define COMMAND_TURN_LEFT 3
#define COMMAND_TURN_RIGHT 4
#define COMMAND_MELODY 5
#define COMMAND_BLINK 6

// PINS
int start_button = 18;

int motors_left_forward = 6;
int motors_left_backwards = 5;

int motors_right_forward = 10;
int motors_right_backwards = 9;

int led_forward = 7;
int led_backwards = 19;
int led_left = 15;
int led_right = 8;

int poti_straight = 16;
int poti_turning = 17;

int buzzer = 14;

// HELPER VARIABLES
int button_status = 0;
int straight_speed = 90;
int turning_speed = 120;
int poti_straight_value = 0;
int poti_turning_value = 0;
float straight_percent = 0;
float turning_percent = 0;
int straight_millis = 0;
int turning_millis = 0;

// STATE
bool running = false;

int commands[256];
int command_index = 0;
int command_count = 0;

void run_motors(int motors_pin_forward, int motors_pin_backwards, bool forward, int motor_speed) {
  digitalWrite(forward ? motors_pin_backwards : motors_pin_forward, LOW);
  analogWrite(forward ? motors_pin_forward : motors_pin_backwards, motor_speed);
}

void stop_motors(int motors_pin_forward, int motors_pin_backwards) {
  digitalWrite(motors_pin_backwards, LOW);
  analogWrite(motors_pin_forward, LOW);
}

// FUNCTIONS
void left_forward(int motor_speed) {
  run_motors(motors_left_forward, motors_left_backwards, true, motor_speed);
}

void left_backwards(int motor_speed) {
  run_motors(motors_left_forward, motors_left_backwards, false, motor_speed);
}

void left_stop() {
  stop_motors(motors_left_forward, motors_left_backwards);
}

void right_forward(int motor_speed) {
  run_motors(motors_right_forward, motors_right_backwards, true, motor_speed);
}

void right_backwards(int motor_speed) {
  run_motors(motors_right_forward, motors_right_backwards, false, motor_speed);
}

void right_stop() {
  stop_motors(motors_right_forward, motors_right_backwards);
}

void forward(int millis) {
  left_forward(straight_speed);
  right_forward(straight_speed);
  digitalWrite(led_forward, HIGH);
  delay(millis);
  left_stop();
  right_stop();
  digitalWrite(led_forward, LOW);
}

void backwards(int millis) {
  left_backwards(straight_speed);
  right_backwards(straight_speed);
  digitalWrite(led_backwards, HIGH);
  delay(millis);
  left_stop();
  right_stop();
  digitalWrite(led_backwards, LOW);
}

void turn_left(int millis) {
  left_forward(turning_speed);
  right_backwards(turning_speed);
  digitalWrite(led_left, HIGH);
  delay(millis);
  left_stop();
  right_stop();
  digitalWrite(led_left, LOW);
}

void turn_right(int millis) {
  left_backwards(turning_speed);
  right_forward(turning_speed);
  digitalWrite(led_right, HIGH);
  delay(millis);
  left_stop();
  right_stop();
  digitalWrite(led_right, LOW);
}

void melody_command() {
  tone(buzzer, NOTE_G5, 125);
  delay(125);
  tone(buzzer, NOTE_G5, 125);
  delay(125);
  tone(buzzer, NOTE_A5, 125);
  delay(125);
  tone(buzzer, NOTE_A5, 125);
  delay(125);
  tone(buzzer, NOTE_E5, 125);
  delay(125);
  tone(buzzer, NOTE_E5, 125);
  delay(125);
  tone(buzzer, NOTE_G5, 125);
  delay(250);

  tone(buzzer, NOTE_G5, 125);
  delay(125);
  tone(buzzer, NOTE_G5, 125);
  delay(125);
  tone(buzzer, NOTE_A5, 125);
  delay(125);
  tone(buzzer, NOTE_A5, 125);
  delay(125);
  tone(buzzer, NOTE_E5, 125);
  delay(125);
  tone(buzzer, NOTE_E5, 125);
  delay(125);
  tone(buzzer, NOTE_G5, 125);
  delay(250);

  tone(buzzer, NOTE_G5, 125);
  delay(125);
  tone(buzzer, NOTE_G5, 125);
  delay(125);
  tone(buzzer, NOTE_A5, 125);
  delay(125);
  tone(buzzer, NOTE_A5, 125);
  delay(125);
  tone(buzzer, NOTE_C6, 125);
  delay(125);
  tone(buzzer, NOTE_C6, 125);
  delay(125);
  tone(buzzer, NOTE_B5, 125);
  delay(250);
  tone(buzzer, NOTE_B5, 125);
  delay(250);
  tone(buzzer, NOTE_A5, 125);
  delay(250);
  tone(buzzer, NOTE_G5, 125);
  delay(250);
  tone(buzzer, NOTE_F5, 125);
  delay(250);
}

void blink_command() {
    digitalWrite(led_forward, HIGH);
    digitalWrite(led_backwards, HIGH);
    digitalWrite(led_left, HIGH);
    digitalWrite(led_right, HIGH);
    delay(500);
    digitalWrite(led_forward, LOW);
    digitalWrite(led_backwards, LOW);
    digitalWrite(led_left, LOW);
    digitalWrite(led_right, LOW);
    delay(500);

    digitalWrite(led_forward, HIGH);
    digitalWrite(led_backwards, HIGH);
    digitalWrite(led_left, HIGH);
    digitalWrite(led_right, HIGH);
    delay(500);
    digitalWrite(led_forward, LOW);
    digitalWrite(led_backwards, LOW);
    digitalWrite(led_left, LOW);
    digitalWrite(led_right, LOW);
    delay(500);

    digitalWrite(led_forward, HIGH);
    digitalWrite(led_backwards, HIGH);
    digitalWrite(led_left, HIGH);
    digitalWrite(led_right, HIGH);
    delay(500);
    digitalWrite(led_forward, LOW);
    digitalWrite(led_backwards, LOW);
    digitalWrite(led_left, LOW);
    digitalWrite(led_right, LOW);
    delay(500);
}

void boot_melody() {
  tone(buzzer, NOTE_G4, 100);
  delay(100);
  tone(buzzer, NOTE_AS4, 100);
  delay(100);
  tone(buzzer, NOTE_DS5, 250);
  delay(250);
}

void run_melody() {
  tone(buzzer, NOTE_DS5, 250);
  delay(1000);
  tone(buzzer, NOTE_DS5, 250);
  delay(1000);
  tone(buzzer, NOTE_DS5, 250);
  delay(1000);
  tone(buzzer, NOTE_DS6, 500);
  delay(500);
}

// SETTING UP
void setup() {
  pinMode(start_button, INPUT);

  pinMode(motors_left_forward, OUTPUT);
  pinMode(motors_left_backwards, OUTPUT);

  pinMode(motors_right_forward, OUTPUT);
  pinMode(motors_right_backwards, OUTPUT);

  pinMode(led_left, OUTPUT);
  pinMode(led_right, OUTPUT);
  pinMode(left_forward, OUTPUT);
  pinMode(left_backwards, OUTPUT);

  pinMode(poti_straight, INPUT);
  pinMode(poti_turning, INPUT);

  pinMode(buzzer, OUTPUT);

  Serial.begin(9600);

  delay(1000);
  boot_melody();

  Serial.println("Let the games begin!");
}

// LOOP
void loop() {
  button_status = digitalRead(start_button);
  if (button_status == HIGH && !running) {
    running = true;

    run_melody();
  }

  poti_straight_value = constrain(analogRead(poti_straight), 0, 1000);
  poti_turning_value = constrain(analogRead(poti_turning), 0, 1000);

  straight_percent = constrain((float) poti_straight_value / 1000, 0.1, 0.9);
  turning_percent = constrain((float) poti_turning_value / 1000, 0.1, 0.9);

  straight_millis = (int) (straight_percent * 5000);
  turning_millis = (int) (turning_percent * 3000);

  if (Serial.available() > 0) {
    String command = Serial.readString();
    command.trim();

    if (command == "left_forward") {
      left_forward(100);
      digitalWrite(led_forward, HIGH);
      delay(2500);
      digitalWrite(led_forward, LOW);
      left_stop();
    } else if (command == "left_backwards") {
      left_backwards(100);
      digitalWrite(led_forward, HIGH);
      delay(2500);
      digitalWrite(led_forward, LOW);
      left_stop();
    } else if (command == "right_forward") {
      right_forward(100);
      digitalWrite(led_forward, HIGH);
      delay(2500);
      digitalWrite(led_forward, LOW);
      right_stop();
    } else if (command == "right_backwards") {
      right_backwards(100);
      digitalWrite(led_forward, HIGH);
      delay(2500);
      digitalWrite(led_forward, LOW);
      right_stop();
    } else if (command == "forward") {
      commands[command_index] = COMMAND_FORWARD;
      command_index++;
    } else if (command == "backwards") {
      commands[command_index] = COMMAND_BACKWARDS;
      command_index++;
    } else if (command == "turn_left") {
      commands[command_index] = COMMAND_TURN_LEFT;
      command_index++;
    } else if (command == "turn_right") {
      commands[command_index] = COMMAND_TURN_RIGHT;
      command_index++;
    } else if (command == "led_forward") {
      digitalWrite(led_forward, HIGH);
      delay(10000);
      digitalWrite(led_forward, LOW);
    } else if (command == "led_backwards") {
      digitalWrite(led_backwards, HIGH);
      delay(10000);
      digitalWrite(led_backwards, LOW);
    } else if (command == "led_left") {
      digitalWrite(led_left, HIGH);
      delay(10000);
      digitalWrite(led_left, LOW);
    } else if (command == "led_right") {
      digitalWrite(led_right, HIGH);
      delay(10000);
      digitalWrite(led_right, LOW);
    } else if (command == "melody") {
      commands[command_index] = COMMAND_MELODY;
      command_index++;
    } else if (command == "blink") {
      commands[command_index] = COMMAND_BLINK;
      command_index++;
    } else if (command == "init") {
      command_index = 0;
    } else if (command == "terminate") {
      command_count = command_index;
      command_index = 0;
    }

    Serial.println("ACK");
  }

  if (running) {
    command_index = 0;
    while (command_index < command_count) {
      if (commands[command_index] == COMMAND_FORWARD) {
        forward(straight_millis);
      } else if (commands[command_index] == COMMAND_BACKWARDS) {
        backwards(straight_millis);
      } else if (commands[command_index] == COMMAND_TURN_LEFT) {
        turn_left(turning_millis);
      } else if (commands[command_index] == COMMAND_TURN_RIGHT) {
        turn_right(turning_millis);
      } else if (commands[command_index] == COMMAND_MELODY) {
        melody_command();
      } else if (commands[command_index] == COMMAND_BLINK) {
        blink_command();
      }

      command_index++;
    }

    running = false;
  }
}
