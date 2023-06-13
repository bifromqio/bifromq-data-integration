# Integration
This is a repo about demonstrates BifroMQ data integration. `Integrator` is used for receiving messages from BifroMQ and 
emitting them to the customized downstream, i.e. `Producer`, through `onMessageArrive()`. In practice, `Producer` can be 
Kafka Producer, MySQL clients etc.  

For [more details](https://github.com/baidu/bifromq-docs/blob/master/website/docs/05_user_guide/2_integration/1_integration.md)
