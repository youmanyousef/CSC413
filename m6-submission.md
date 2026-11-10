1) Who decides, who shows. Checkmate is detected in one class and announced in another. Name both, and say which of session 16's three questions puts each one there.

`GameController` finds out about the status of the game, including `CHECKMATE` and passes on that information to a `BoardView` type.
`GameController` takes a type and tells a view -> Controller. `BoardView` or `ConsoleView` takes inputs from user and shows things to the user -> View.

2) The dependency rule. Which of your classes import edu.sfsu.csc413.chess.view? Which must not, and why?

`GameController` imports views, but that's fine because the controller's job is to manage sending messages to and receiving messages from the view.

`Game` and `MoveGenerator` are examples of Models, and should not have imported view. The controller asks the model for its rules and connects them together, and it sends the message to the view.

3) Test 2 on your engine. In M7 you add a Swing view. List the files you expect to change. If your list includes GameController or anything in engine, say what in your design makes that necessary.

No files will change. As long as `SwingView` adheres to the `BoardView` interface, then it should work without having to change much of the controller or model code. 